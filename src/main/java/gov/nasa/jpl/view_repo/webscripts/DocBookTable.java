package gov.nasa.jpl.view_repo.webscripts;

public class DocBookTable {
	private int colCount;
	private int headerRowCount;
	private int bodyRowCount;
	private int header[][];
	private int body[][];
	
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

	public void setColCount(int colCount){this.colCount = colCount;}
	
	public void setHeaderRowCount(int rowCount){headerRowCount = rowCount;}
	
}
