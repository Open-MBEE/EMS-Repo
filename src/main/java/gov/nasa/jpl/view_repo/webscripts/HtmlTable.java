package gov.nasa.jpl.view_repo.webscripts;

import java.util.Arrays;
import java.util.UUID;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sun.star.util.DateTime;

public class HtmlTable {
	private int colCount;
	private int headerRowCount;
	private int footerRowCount;
	private int bodyRowCount;
	private int header[][];
	private int body[][];
	private int footer[][];
	private String title;
	private boolean hasHeader;
	private boolean hasFooter;
	private boolean hasTitle;
	private Element table;
	private Elements headerRows;
	private Elements footerRows;
	private Elements bodyRows;
	private SnapshotPost snapshotPostService;

	public enum TablePart{
		BODY,
		FOOTER,
		HEADER
	}

	public HtmlTable(Element table, SnapshotPost snapshotPostService){
		if(table == null) return;
		if(table.tagName().compareToIgnoreCase("table") != 0) return;
		this.table = table;
		this.snapshotPostService = snapshotPostService;
		
		int max = 0;
		int curMax;
		
		//looking for table title/caption
		Elements caption = table.select(" > caption");
		if(caption == null || caption.size()==0) this.hasTitle = false;
		else{
			this.hasTitle = true;
			this.title = String.valueOf(caption.first().text());	//TODO encode HTML
		}
		
		//looking for table header
		Elements thead = table.select(" > thead");
		if(thead == null || thead.size()==0) this.hasHeader = false;
		else{
			this.hasHeader = true;
			this.headerRows = thead.select("tr");
			this.headerRowCount = this.headerRows.size();
			curMax = getColumnMax(this.headerRows);
			if(max < curMax) max = curMax; 
		}
		
		//looking for table footer
		Elements tfoot = table.select(" > tfoot");
		if(tfoot == null || tfoot.size()==0) this.hasFooter = false;
		else{
			this.hasFooter = true;
			this.footerRows = tfoot.select("tr");
			this.footerRowCount = this.footerRows.size();
			curMax = getColumnMax(this.footerRows);
			if(max < curMax) max = curMax;
		}
		
		//looking for table tbody
		Elements tbody = table.select(" > tbody");
		if(tbody == null || tbody.size()==0){
//			if(!this.hasHeader){
//				this.headerRows = table.select("table > tr").first().select("tr");
//				this.headerRowCount = this.headerRows.size();
//				this.hasHeader = true;
//				table.select("table > tr").first().remove();
//			}
			this.bodyRows = table.select("table > tr");
			this.bodyRowCount = this.bodyRows.size();
			curMax = getColumnMax(this.bodyRows);
			if(max < curMax) max = curMax;
		}
		else{
//			if(!this.hasHeader){
//				this.headerRows = tbody.select(" > tr").first().select("tr");
//				this.headerRowCount = this.headerRows.size();
//				this.hasHeader = true;
//				tbody.select(" > tr").first().remove();
//			}
			this.bodyRows = tbody.select(" > tr");
			this.bodyRowCount = this.bodyRows.size();
			curMax = getColumnMax(this.bodyRows);
			if(max < curMax) max = curMax;
		}
		
		this.colCount = max;
		this.init();
	}

	private String buildElementEntry(int row, int col, int startCol, Element cell, TablePart tablePart){
		StringBuffer sb = new StringBuffer();

		int rowspan = 0;
		int colspan = 0;
		
		String rspan = cell.attr("rowspan");
		String cspan = cell.attr("colspan");
		
		if(rspan != null && !rspan.isEmpty()) rowspan = Integer.parseInt(rspan);
		if(cspan != null && !cspan.isEmpty()) colspan = Integer.parseInt(cspan);
		
		startCol = this.getStartCol(row, col, rowspan, colspan, tablePart);
		
		if(startCol > this.colCount) return "";
		
		String s = this.snapshotPostService.handleTransclusion(UUID.randomUUID().toString(), "unknown", cell.html());
		s = this.snapshotPostService.handleEmbeddedImage(s);
		s = this.snapshotPostService.HtmlSanitize( s );
		
		if(rowspan > 1 || colspan > 1){
			sb.append("<entry");
			
			if(rowspan > 1){
				sb.append(String.format(" morerows=\"%d\"", rowspan-1));
			}
			
			if(colspan > 1){ 
				int end = startCol + colspan - 1;
        		if(end > this.getColCount()) end = this.getColCount();
        		sb.append(String.format(" namest=\"%d\" nameend=\"%d\"", startCol, end));
			}
			sb.append(String.format(">%s</entry>", s));
		}
		else 
			sb.append(String.format("<entry>%s</entry>", s));
		
		return sb.toString();
	}
	
	private String generateColSpec(int count){
    	StringBuffer sb = new StringBuffer();
    	int index = 1;
    	while(index <= count){
    		sb.append(String.format("<colspec colname=\"%d\" colnum=\"%d\"/>", index, index));
    		index++;
    	}
    	return sb.toString();
    }

	private String generateBody(){
		StringBuffer sb = new StringBuffer();
		Elements bodyRows = this.bodyRows;
		int startCol = 1;
		
		sb.append("<utbody>");
		for(int row=0; row < bodyRows.size(); row++){
			Element tr = bodyRows.get(row);
			sb.append("<row>");
			Elements cells = tr.select(" > td");
			if(cells == null || cells.size() == 0) cells = tr.select(" > th");
			if(cells != null && cells.size() > 0){
				for(int col = 0; col < cells.size(); col++){
					sb.append(buildElementEntry(row, col, startCol, cells.get(col), TablePart.BODY));
				}
			}
			sb.append("</row>");
		}
		handleRowsDifferences(this.body, this.bodyRowCount, sb);
		sb.append("</utbody>");
		return sb.toString();

	}
	
	private String generateFooter(){
		if(!this.hasFooter) return "";
		
		StringBuffer sb = new StringBuffer();		
		sb.append("<utfoot>");
		
		int startCol = 1;
		
		for(int row = 0; row < footerRows.size(); row++){
			Element tr = footerRows.get(row);
			sb.append("<row>");
			Elements cells = tr.select(" > th");
			if(cells == null || cells.size() == 0) cells = tr.select(" > td");
			if(cells != null && cells.size() > 0){
				for(int col = 0; col < cells.size(); col++){
					sb.append(buildElementEntry(row, col, startCol, cells.get(col), TablePart.FOOTER));
				}
			}
			sb.append("</row>");
		}
		handleRowsDifferences(this.footer, this.footerRowCount, sb);
		sb.append("</utfoot>");
		return sb.toString();
	}

	private String generateHeader(){
		if(!this.hasHeader) return "";
		
		StringBuffer sb = new StringBuffer();		
		sb.append("<uthead>");
		
		int startCol = 1;
		
		if(this.hasHeader){
			for(int row = 0; row < headerRows.size(); row++ ){
				Element tr = headerRows.get(row);
				sb.append("<row>");
				Elements cells = tr.select(" > th");
				if(cells == null || cells.size() == 0) cells = tr.select(" > td");
				if(cells != null && cells.size() > 0){
					for(int col = 0; col < cells.size(); col++){
						sb.append(buildElementEntry(row, col, startCol, cells.get(col), TablePart.HEADER));
					}
				}
				sb.append("</row>");
			}
			handleRowsDifferences(this.header, this.headerRowCount, sb);
		}
		else{
			Element tr = this.bodyRows.first();
			Elements cells = tr.select(" > td");
			if(cells == null || cells.size() == 0) cells = tr.select(" > th");
			sb.append("<row>");
			if(cells != null && cells.size() > 0){
				for(int col = 0; col < cells.size(); col++){
					sb.append(buildElementEntry(0, col, startCol, cells.get(col), TablePart.BODY));
				}
			}
			sb.append("</row>");
		}
		sb.append("</uthead>");
		return sb.toString();
	}
	
	private int getColumnMax(Elements TRs){
		int max=0;
		int curMax=0;
		for(Element tr : TRs){
			curMax=0;
			Elements TDs = tr.select(" > td");
			if(TDs != null && TDs.size() > 0){
//				curMax = TDs.size();
//				if(curMax > tr.children().size()) curMax = tr.children().size();
//				if(max < curMax) max = curMax;
				
				for(Element td : TDs){
					if(td.hasAttr("colspan")){
						try{
							int col = Integer.parseInt(td.attr("colspan"));
							curMax += col;
						}
						catch(NumberFormatException ex){
							curMax++;
						}
					}
					else{
						curMax++;
					}
				}
//				if(curMax > tr.children().size()) curMax = tr.children().size();
				if(max < curMax) max = curMax;
			}
		}
		return max;
	}
	
	public int[][] getBody(){return body;}
	
	public int getBodyRowCount(){return bodyRowCount;}
	
	public int getColCount(){ return colCount; }
	
	public int getFooterRowCount() { return this.footerRowCount; }
	
	public int[][] getHeader(){return header;}

	public int getHeaderRowCount(){return headerRowCount;}

	private int getPrevStartCol(int row, int col, TablePart tablePart){
		int startCol = 1;
		int markedRow[] = null;
		
		row++;
		col++;
		
		switch(tablePart){
		case BODY:
			markedRow = this.body[row];
			break;
		case FOOTER:
			markedRow = this.footer[row];
			break;
		case HEADER:
			if(this.hasHeader)
				markedRow = this.header[row];
			else
				markedRow = this.body[row];
			break;
		}
		
		for(int i=col; i <= markedRow.length; i++){
			if(i >= markedRow.length) return markedRow.length-1;
			if(markedRow[i]==-1){
				startCol = i;
				break;
			}
		}
		return startCol;
	}
	
	private int getStartCol(int row, int col, int rowspan, int colspan, TablePart tablePart){
		int rows = (rowspan > 0) ? rowspan : 1;
		int startCol = getPrevStartCol(row, col, tablePart);
		int moreRows = (rowspan > 0) ? rowspan-1 : 0;
		int namest = startCol;
		int nameend = startCol + ((colspan > 0) ? colspan-1 : 0);
		// header had used body's 1st row
//		if(!this.hasHeader && tablePart==TablePart.BODY) row++;
		
		for(int i=moreRows+row+1; i > row; i--){
			for(int j=nameend; j >= namest; j--){
				switch(tablePart){
				case HEADER:
					if(i <= headerRowCount && j <= colCount){
						if(header[i][j] == -1){
							header[i][j] = rows;
							rows = 0;
						}
					}
					break;
				case BODY:
					if(i <= bodyRowCount && j <= colCount){
						if(body[i][j] == -1){
							body[i][j] = rows;
							rows=0;
						}
					}
					break;
				case FOOTER:
					if(i <= footerRowCount && j <= colCount){
						if(footer[i][j] == -1){
							footer[i][j] = rows;
							rows = 0;
						}
					}
					break;
				}
			}
		}
		return startCol;
	}

	public String getTitle(){
		if(this.title == null || this.title.isEmpty()) return "";
		return this.title;
	}

	/*
	 * handles a situation where HTML table defines more rowspan than the number of rows it has.
	 */
	private void handleRowsDifferences(int[][] rowspan, int rows, StringBuffer sb){
		// tallies up rowspan
		int rowspanCount[] = new int[colCount];
		for(int i=1; i < colCount; i++){
			int total = 0;
			int row = 0;
			for(int j=1; j < rowspan.length; j++){
				row = rowspan[j][i];
				if(row > -1) total += rowspan[j][i];
			}
			rowspanCount[i] = total;
		}
		// adds more row for any discrepancy
		for(int i=0; i < rowspanCount.length; i++){
			if(rowspanCount[i] >= rows){
				for(int j=rowspanCount[i]-rows; j>0;j--){
					sb.append("<row><entry/></row>");
				}
			}
		}
	}
	
	public boolean hasTitle() { return this.hasTitle; }
	
	public boolean hasHeader() { return this.hasHeader; }
	
	public boolean hasFooter() { return this.hasFooter; }
	
	public void init(){
		header = new int[headerRowCount+1][colCount+1];
		body = new int[bodyRowCount+1][colCount+1];
		footer = new int[footerRowCount+1][colCount+1];
//		bodyRowspanCount = new int[bodyRowCount][colCount];
//		footerRowspanCount = new int[footerRowCount][colCount];
//		headerRowspanCount = new int[headerRowCount][colCount];
		initArray(header);
		initArray(footer);
		initArray(body);
	}

	private void initArray(int[][] array){
//		Arrays.fill(array, -1);
		for(int i=0; i < array.length; i++){
			for(int j=0; j < array[i].length; j++)
				array[i][j] = -1;
		}
	}
	
	public String toDocBook(){
		StringBuffer sb = new StringBuffer();
		sb.append("<utable frame=\"all\" pgwide=\"1\" role=\"longtable\" tabstyle=\"normal\">");
		sb.append(String.format("<title>%s</title>", (this.hasTitle ? this.title : "")));
		sb.append(String.format("<tgroup cols=\"%d\" align=\"left\" colsep=\"1\" rowsep=\"1\">", this.getColCount()));
		sb.append(generateColSpec(this.colCount));
		sb.append(generateHeader());
		sb.append(generateFooter());
		sb.append(generateBody());
		sb.append("</tgroup>");
		sb.append("</utable>");
		return sb.toString();
	}
}
