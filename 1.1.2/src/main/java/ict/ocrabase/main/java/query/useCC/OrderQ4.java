package ict.ocrabase.main.java.query.useCC;

import ict.ocrabase.main.java.client.index.IndexNotExistedException;
import ict.ocrabase.main.java.client.index.IndexResultScanner;
import ict.ocrabase.main.java.client.index.IndexTable;
import ict.ocrabase.main.java.client.index.Range;
import ict.ocrabase.main.java.regionserver.DataType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.util.Bytes;

import sun.tools.tree.ThisExpression;

/**
 * select orderpriority, count(*) as order_count from orders where orderdate >= ? orderdate < ? group by orderpriority
 * @author houliang
 *
 */
public class OrderQ4 {
	
	
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

	public static void queryTest(String startDate, String endDate,
			String saveFile, String tableName, int scanCache, int threads)
			throws IOException {
		File datasource = new File(saveFile);
		FileWriter fileWriter;
		try {
			datasource.createNewFile();
			fileWriter = new FileWriter(datasource, true);
		} catch (IOException e) {
			System.err.println("create file failed");
			e.printStackTrace();
		}

		IndexTable indextable = new IndexTable(tableName);
		indextable.setScannerCaching(scanCache);
		indextable.setMaxScanThreads(threads);
		// System.out.println("max thread:" + indextable.getMaxScanThreads());

		Range[] ranges = new Range[1];
		ranges[0] = new Range(indextable.getTableName(), Bytes.toBytes("f:c4"));
		ranges[0].setStartType(CompareOp.GREATER_OR_EQUAL);
		ranges[0].setStartValue(Bytes.toBytes(startDate));
		ranges[0].setEndType(CompareOp.LESS);
		ranges[0].setEndValue(Bytes.toBytes(endDate));

		byte[][] resultcolumn = new byte[1][];
		resultcolumn[0] = Bytes.toBytes("f:c5");
		try {
			IndexResultScanner rs = indextable.getScanner(
					new Range[][] { ranges }, resultcolumn);

			Result r;

			Map<byte[], DataType> columnMap = indextable.getColumnInfoMap();

			// System.out.println(rs.getTotalScannerNum() +
			// "   "+rs.getTotalCount() +"  "+rs.getFinishedScannerNum());
			long count = 0;
//			fileWriter = new FileWriter(datasource);
			ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<String, Long>();
			while ((r = rs.next()) != null) {
				String orderPriority = new String(r.getValue(
						Bytes.toBytes("f"), Bytes.toBytes("c5")));
				if (map.get(orderPriority) == null) {
					map.put(orderPriority, 1l);
				} else {
					map.put(orderPriority, map.get(orderPriority) + 1);
				}
				count++;
//				 fileWriter.write(println_test(r)+"\n");
//				 fileWriter.flush();
			}
			write2file(map, saveFile);
//			fileWriter.close();
		} catch (IndexNotExistedException e) {
			System.err.println("error query");
			e.printStackTrace();
		}

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

	public static void main(String[] args) throws IOException {
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
		queryTest(startDate, endDate, saveFile, tableName, scanCache, threads);
		long endTime = System.currentTimeMillis();
		System.out.println("endtime - starttime = " + (endTime - startTime)
				+ " ms");

	}
}
