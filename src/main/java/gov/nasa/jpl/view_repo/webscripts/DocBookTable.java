package gov.nasa.jpl.view_repo.webscripts;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpl.docbook.model.DBTableEntry;
import gov.nasa.jpl.docbook.model.DocumentElement;

public class DocBookTable {
	private int colCount;
	public int headerRowCount;
	public int bodyRowCount;
	public int header[][];
	public int body[][];
	
	public int[][] getBody(){return body;}
	
	public int getBodyRowCount(){return bodyRowCount;}
	
	public int getColCount(){ return colCount;}
	
	public int[][] getHeader(){return header;}

	public int getHeaderRowCount(){return headerRowCount;}

	private int getPrevStartCol(int row, int col, boolean isHeader){
		int startCol = 1;
		int markedRow[] = null;
		
		if(isHeader){
			markedRow = this.header[row];
		}
		else{
			markedRow = this.body[row];
		}
		
		for(int i=col; i < markedRow.length; i++){
			if(i >= markedRow.length) return markedRow.length;
			if(markedRow[i]==-1){
				startCol = i+1;
				break;
			}
		}
		return startCol;
	}
	
	public int getStartCol(int row, int col, int rowspan, int colspan, boolean isHeader){
//		int startCol = 0;
//		boolean isSet = false;
//		int rowEnd = row + rowspan;
//		int colEnd = col + colspan;
//		for(int i = row; i < rowEnd; i++){
//			for(int j = col; j < this.colCount; j++){
//				if(isHeader){
//					if(i < headerRowCount && j < colCount){
//						if(header[i][j] == 0 && !isSet){
//							startCol = j+1;
//							isSet = true;
//							header[i][j] = 1;
//						}
//						if(j < colEnd) header[i][j] = 1;
//					}
//				}
//				else{
//					if(i < bodyRowCount && j < colCount){
//						if(body[i][j] == 0 && !isSet){
//							startCol = j+1;
//							isSet = true;
//							body[i][j] = 1;
//						}
//						if(j < colEnd) body[i][j] = 1;
//					}
//				}
//			}
//		}
//		return startCol;
		
		int rows = (rowspan > 0) ? rowspan : 1;
		int startCol = getPrevStartCol(row, col, isHeader);
		int rowEnd = row + rowspan;
		int colEnd = col + colspan;
		
		for(int i = row; i < rowEnd; i++){
			for(int j = col; j < colEnd; j++){
				if(isHeader){
					if(i < headerRowCount && j < colCount){
						if(header[i][j] == -1){
							header[i][j] = rows;
							rows = 0;
						}
					}
				}
				else{
					if(i < bodyRowCount && j < colCount){
						if(body[i][j] == -1){
							body[i][j] = rows;
							rows = 0;
						}
					}
				}
			}
		}
		return startCol;
	}
	
	/*
	 * handles a situation where HTML table defines more rowspan than the number of rows it has.
	 */
	public void handleRowsDifferences(int[][] rowspan, int rowCount, List< List< DocumentElement >> elemList){
		// tallies up rowspan
		int rowspanCount[] = new int[colCount];
		for(int i=0; i < colCount; i++){
			int total = 0;
			for(int j=0; j < rowspan.length; j++){
				total += rowspan[j][i];
			}
			rowspanCount[i] = total;
		}
		
		// adds more row for any discrepancy
		for(int i=0; i < rowspanCount.length; i++){
			if(rowspanCount[i] > rowCount){
				for(int j=rowspanCount[i]- rowCount; j>0;j--){
					List< DocumentElement > rows = new ArrayList< DocumentElement >();
					DBTableEntry te = new DBTableEntry();
					rows.add(te);
					elemList.add(rows);
				}
			}
		}
	}
	
	public void init(){
		header = new int[headerRowCount][colCount];
		body = new int[bodyRowCount][colCount];
		initArray(header);
		initArray(body);
	}
	
	private void initArray(int[][] array){
//		Arrays.fill(array, -1);
		for(int i=0; i < array.length; i++){
			for(int j=0; j < array[i].length; j++)
				array[i][j] = -1;
		}
	}

	public void setBodyRowCount(int rowCount){bodyRowCount = rowCount;}

	public void setColCount(int colCount){this.colCount = colCount;}
	
	public void setHeaderRowCount(int rowCount){headerRowCount = rowCount;}
}
