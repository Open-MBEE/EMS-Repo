package gov.nasa.jpl.view_repo.webscripts;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpl.docbook.model.DBTableEntry;
import gov.nasa.jpl.docbook.model.DocumentElement;
import gov.nasa.jpl.view_repo.webscripts.HtmlTable.TablePart;

public class DocBookTable {
	private int colCount;
	public int headerRowCount;
	public int bodyRowCount;
	private int header[][];
	private int body[][];
	public int headerRowspanCount[][];
	public int bodyRowspanCount[][];
	
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
		bodyRowspanCount = new int[bodyRowCount][colCount];
		headerRowspanCount = new int[headerRowCount][colCount];
	}
	
	public void setBodyRowCount(int rowCount){bodyRowCount = rowCount;}

	public void setColCount(int colCount){this.colCount = colCount;}
	
	public void setHeaderRowCount(int rowCount){headerRowCount = rowCount;}
	
	/*
	 * keeps tab on HTML table rowspan so we can accommodate any discrepancies later
	 */
	public void tracksRowspanCount(int rowspan, int row, int col, TablePart tablePart){
		if(rowspan < 1) rowspan = 0;
		switch(tablePart){
		case BODY:
			this.bodyRowspanCount[row][col] = rowspan;
			break;
//		case FOOTER:
//			this.footerRowspanCount[row][col] = rowspan;
//			break;
		case HEADER:
			this.headerRowspanCount[row][col] = rowspan;
			break;
		}
	}
}
