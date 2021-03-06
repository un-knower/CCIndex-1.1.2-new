package ict.ocrabase.main.java.client.bulkload.index;

import ict.ocrabase.main.java.client.bulkload.BulkLoadUtil;
import ict.ocrabase.main.java.client.bulkload.ImportConstants;
import ict.ocrabase.main.java.client.bulkload.KeyValueArray;
import ict.ocrabase.main.java.client.bulkload.TableInfo;
import ict.ocrabase.main.java.client.index.IndexNotExistedException;
import ict.ocrabase.main.java.client.index.IndexSpecification.IndexType;
import ict.ocrabase.main.java.client.index.IndexTableDescriptor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Used for importing data and index into the table, transfer text to KeyValue array
 * @author gu
 *
 */
public class IndexFromTextReducer extends
		Reducer<ImmutableBytesWritable, Text, Text, KeyValueArray> {
	private int indexPos;
	private Text tableName;
	private TableInfo table;
	private IndexType indexType = null;
	private int count = 0;
	
	private int timestampPos;
	
	protected void setup(Context context) throws IOException,
			InterruptedException {
		table = new TableInfo(context.getConfiguration().get(
				ImportConstants.BULKLOAD_DATA_FORMAT));
		this.timestampPos = table.getTimestampPos();
		if(indexPos == 0)
			tableName = new Text(table.getTableName());
		else{
			TableInfo.ColumnInfo info = table.getColumnInfo(table.getIndexPos(indexPos-1));
			tableName = new Text(info.getIndexTableName());
			
			byte[] htdBytes = Bytes.toBytesBinary(context.getConfiguration().get("bulkload.indexTableDescriptor"));
			HTableDescriptor htd = new HTableDescriptor();
			htd.readFields(new DataInputStream(new ByteArrayInputStream(htdBytes)));
			try {
				indexType = table.convertToIndexTableInfo(new IndexTableDescriptor(htd), Bytes.toString(info.getFamily()), Bytes.toString(info.getQualifier()));
			} catch (IndexNotExistedException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void reduce(
			ImmutableBytesWritable row,
			Iterable<Text> str,
			Reducer<ImmutableBytesWritable, Text, Text, KeyValueArray>.Context context)
			throws IOException, InterruptedException {
		byte[] keyBytes = new byte[row.getLength()-1];
		Bytes.putBytes(keyBytes, 0, row.get(), row.getOffset()+1, row.getLength()-1);
		
		TreeSet<KeyValue> kvList = new TreeSet<KeyValue>(KeyValue.COMPARATOR);
		Iterator<Text> it = str.iterator();
		while(it.hasNext()){
			Text text = it.next();
			byte[] line = text.getBytes();
			int lineLength = text.getLength();
			Integer[] split = BulkLoadUtil.dataSplit(table.getSeparator(),line,lineLength);
			
			byte[] ts = null;
			if(this.timestampPos != -1){
				ts = Bytes.toBytes(Long.valueOf(Bytes.toString(line, split[timestampPos]+1, split[timestampPos+1]-split[timestampPos]-1)));
			}
			
			
	
			byte[] keyvalue = null;
			for (TableInfo.ColumnInfo ci : table.getColumnInfo()) {
				int i = ci.getPos();
	
				keyvalue = BulkLoadUtil.createKVByte(keyBytes, ci.bytes, ci.getDataType(), line, split[i]+1, split[i+1]-split[i]-1);
				KeyValue kv = new KeyValue(keyvalue, 0, keyvalue.length);
				if(this.timestampPos != -1){
          // kv.updateStamp(ts);
          int tsOffset = kv.getTimestampOffset();
          System.arraycopy(ts, 0, kv.getBuffer(), tsOffset, Bytes.SIZEOF_LONG);
				}
				i++;
	
				kvList.add(kv);
			}
		}
		context.write(tableName, new KeyValueArray(kvList));

		count++;
		if(count % 10000 == 0)
			context.setStatus("Write " + count);
	}
	
	/**
	 * Used for secondary index
	 * @param row
	 * @param context
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected void reduce(
			ImmutableBytesWritable row,
			Reducer<ImmutableBytesWritable, Text, Text, KeyValueArray>.Context context)
			throws IOException, InterruptedException {
		byte[] keyBytes = new byte[row.getLength()-1];
		Bytes.putBytes(keyBytes, 0, row.get(), row.getOffset()+1, row.getLength()-1);
		
		TableInfo.ColumnInfo ci = table.getColumnInfo(0);
		byte[] keyvalue = BulkLoadUtil.createKVByte(keyBytes, ci.bytes, null, 0, 0);
		KeyValue kv = new KeyValue(keyvalue, 0, keyvalue.length);
		
		context.write(tableName, new KeyValueArray(new KeyValue[]{kv}));
		
		count++;
		if(count % 10000 == 0)
			context.setStatus("Read" + count);
	}
	
	public void run(Context context) throws IOException, InterruptedException {
		if(context.nextKey()){
			ImmutableBytesWritable row = context.getCurrentKey();
			indexPos = (int)row.get()[row.getOffset()];
			setup(context);
			if(indexType != null && indexType == IndexType.SECONDARYINDEX){
				do {
					reduce(context.getCurrentKey(), context);
				} while (context.nextKey());
			}else{
				do{
					reduce(context.getCurrentKey(), context.getValues(), context);
				} while (context.nextKey());
			}
			cleanup(context);
		}
	}
}
