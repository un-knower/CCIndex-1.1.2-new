package ict.ocrabase.main.java.query.nouseCC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * select orderpriority, count(*) as order_count from orders where orderdate >= ? orderdate < ? group by orderpriority
 * 
 * @author houliang
 *
 */
public class NoOrderQ4 {
	private static Configuration conf;
	private static final byte[] table_test = Bytes
			.toBytes("real_table_with_index");
	private static long startTime = 0;
	private static long stopTime = 0;
	static int count = 0;

	public void queryByRowKey(String rowKey) throws IOException {
		conf = HBaseConfiguration.create();
		HTable table = new HTable(conf, table_test);
		Get get = new Get(rowKey.getBytes());
		Result r = table.get(get);
		System.out.println("rowkey: " + new String(r.getRow()));
		for (KeyValue kv : r.raw()) {
			System.out.println("column: "
					+ new String(kv.getFamily() + " ====value:  "
							+ new String(kv.getValue())));
		}
	}

	public void queryByColumnValue(String columnValue) throws IOException {
		conf = HBaseConfiguration.create();
		HTable table = new HTable(conf, table_test);
		Filter filter = new SingleColumnValueFilter(Bytes.toBytes("f"),
				Bytes.toBytes("c4"), CompareOp.EQUAL,
				Bytes.toBytes(columnValue));
		Scan s = new Scan();
		s.setFilter(filter);
		ResultScanner rs = table.getScanner(s);

		for (Result r : rs) {
			System.out.println("rowkey: " + new String(r.getRow()));
			for (KeyValue kv : r.raw()) {
				System.out.println("column: " + new String(kv.getFamily())
						+ "====value: " + new String(kv.getValue()));
			}
		}
	}

	public void queryByColumnRange(String startDate, String endDate,
			String saveFile, String tableName, int scanCache, int threads) throws IOException {
		conf = HBaseConfiguration.create();
		HTable table = new HTable(conf, tableName);
		List<Filter> filters = new ArrayList<Filter>();

		Filter filter1 = new SingleColumnValueFilter(Bytes.toBytes("f"),
				Bytes.toBytes("c4"), CompareOp.GREATER_OR_EQUAL,
				Bytes.toBytes(startDate));
		filters.add(filter1);
		
		Filter filter2 = new SingleColumnValueFilter(Bytes.toBytes("f"),
				Bytes.toBytes("c4"), CompareOp.LESS,
				Bytes.toBytes(endDate));
		filters.add(filter2);


		Scan s = new Scan();
		s.setCaching(scanCache);
		FilterList filterList1 = new FilterList(filters);

		s.setFilter(filterList1);

		ResultScanner rs = table.getScanner(s);
		long count = 0;
		ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<String, Long>();
		for (Result r : rs) {
			String orderPriority = new String(r.getValue(
					Bytes.toBytes("f"), Bytes.toBytes("c5")));
			if (map.get(orderPriority) == null) {
				map.put(orderPriority, 1l);
			} else {
				map.put(orderPriority, map.get(orderPriority) + 1);
			}
		}
		write2file(map, saveFile);
	}
	
	public static void write2file(ConcurrentHashMap<String, Long> map,
			String saveFile) {
		File datasource = new File(saveFile);
		try {
			datasource.createNewFile();
		} catch (IOException e) {
			System.err.println("create file failed");
			e.printStackTrace();
		}
		try {
			FileWriter fileWriter = new FileWriter(datasource);
			for(Entry<String, Long> entry: map.entrySet()){
				fileWriter.write("f:c5="+entry.getKey()+","+"count="+entry.getValue()+"\n");
			}
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String println_test(Result result) {
		StringBuilder sb = new StringBuilder();
		sb.append("row=" + Bytes.toString(result.getRow()));

		List<KeyValue> kv = result.getColumn(Bytes.toBytes("f"),
				Bytes.toBytes("c1"));
		if (kv.size() != 0) {
			sb.append(", f:c1=" + Bytes.toString(kv.get(0).getValue()));
		}

		kv = result.getColumn(Bytes.toBytes("f"), Bytes.toBytes("c2"));
		if (kv.size() != 0) {
			sb.append(", f:c2=" + Bytes.toString(kv.get(0).getValue()));
		}

		kv = result.getColumn(Bytes.toBytes("f"), Bytes.toBytes("c3"));
		if (kv.size() != 0) {
			sb.append(", f:c3=" + Bytes.toString(kv.get(0).getValue()));
		}

		kv = result.getColumn(Bytes.toBytes("f"), Bytes.toBytes("c4"));
		if (kv.size() != 0) {
			sb.append(", f:c4=" + Bytes.toString(kv.get(0).getValue()));
		}
		kv = result.getColumn(Bytes.toBytes("f"), Bytes.toBytes("c5"));
		if (kv.size() != 0) {
			sb.append(", f:c5=" + Bytes.toString(kv.get(0).getValue()));
		}

		kv = result.getColumn(Bytes.toBytes("f"), Bytes.toBytes("c6"));
		if (kv.size() != 0) {
			sb.append(", f:c6=" + Bytes.toString(kv.get(0).getValue()));
		}
		kv = result.getColumn(Bytes.toBytes("f"), Bytes.toBytes("c7"));
		if (kv.size() != 0) {
			sb.append(", f:c7=" + Bytes.toString(kv.get(0).getValue()));
		}
		kv = result.getColumn(Bytes.toBytes("f"), Bytes.toBytes("c8"));
		if (kv.size() != 0) {
			sb.append(", f:c8=" + Bytes.toString(kv.get(0).getValue()));
		}
		return sb.toString();
	}

	
	
	
	public static void main(String args[]) throws IOException {
		NoOrderQ4 queryByCondition = new NoOrderQ4();

		if (args.length != 6) {
			System.out.println("wrong parameter");
			return;
		}
		String startDate = args[0];
		String endDate = args[1];
		String saveFile = args[2];
		String tableName = args[3];
		int scanCache = Integer.parseInt(args[4]);
		int threads = Integer.parseInt(args[5]);
		System.out.println(startDate + "," + endDate + "," + saveFile + ","
				+ tableName + "," + scanCache + "," + threads);
		long startTime = System.currentTimeMillis();
		queryByCondition.queryByColumnRange(startDate, endDate, saveFile, tableName, scanCache, threads);
		long endTime = System.currentTimeMillis();
		System.out.println("endtime - starttime = " + (endTime - startTime)
				+ " ms");
	}

}
