package gov.nasa.jpl.view_repo.webscripts;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlTable {
	private int colCount;
	private int headerRowCount;
	private int bodyRowCount;
	private int header[][];
	private int body[][];
	
	public HtmlTable(Element table){
		if(table == null) return;
		if(table.tagName().compareToIgnoreCase("table") != 0) return;
		setColumnCount(table);
	}
	
	public int[][] getBody(){return body;}
	
	public int getBodyRowCount(){return bodyRowCount;}
	
	public int getColCount(){ return colCount;}
	
	public int[][] getHeader(){return header;}

	public int getHeaderRowCount(){return headerRowCount;}

	public int getStartCol(int row, int col, int rowspan, int colspan, boolean isHeader){
		int startCol = 0;
		boolean isSet = false;
		int rowEnd = row + rowspan;
		int colEnd = col + colspan;
		for(int i = row; i < rowEnd; i++){
			for(int j = col; j < this.colCount; j++){
				if(isHeader){
					if(i < headerRowCount && j < colCount){
						if(header[i][j] == 0 && !isSet){
							startCol = j+1;
							isSet = true;
							header[i][j] = 1;
						}
						if(j < colEnd) header[i][j] = 1;
					}
				}
				else{
					if(i < bodyRowCount && j < colCount){
						if(body[i][j] == 0 && !isSet){
							startCol = j+1;
							isSet = true;
							body[i][j] = 1;
						}
						if(j < colEnd) body[i][j] = 1;
					}
				}
			}
		}
		return startCol;
	}
	
	public void init(){
		header = new int[headerRowCount][colCount];
		body = new int[bodyRowCount][colCount];
	}
	
	public void setBodyRowCount(int rowCount){bodyRowCount = rowCount;}

	private void setColumnCount(Element table){
		if(table == null) return;
		int max = 0;
		Elements TRs = table.select("tbody > tr");
		for(Element tr : TRs){
			Elements TDs = tr.select("tr > td");
			if(TDs.size() > max) max = TDs.size();
		}
		this.colCount = max;
	}
		
	public void setHeaderRowCount(int rowCount){headerRowCount = rowCount;}
	
}
//
//	private int columnCount;
//	
//	public HtmlTable(Element table){
//		if(table == null) return;
//		if(table.tagName().compareToIgnoreCase("table") != 0) return;
//		setColumnCount(table);
//	}
//	
//	public int getColumnCount(){ return this.columnCount;}
//	
//	private void setColumnCount(Element table){
//		if(table == null) return;
//		int max = 0;
//		Elements TRs = table.select("tbody > tr");
//		for(Element tr : TRs){
//			Elements TDs = tr.select("tr > td");
//			if(TDs.size() > max) max = TDs.size();
//		}
//		this.columnCount = max;
//	}
//}
