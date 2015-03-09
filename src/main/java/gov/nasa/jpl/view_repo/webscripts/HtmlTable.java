package gov.nasa.jpl.view_repo.webscripts;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlTable {
	private int colCount;
	private int headerRowCount;
	private int footerRowCount;
	private int bodyRowCount;
	private int header[][];
	private int body[][];
	private String title;
	private boolean hasHeader;
	private boolean hasFooter;
	private boolean hasTitle;
	private Element table;
	private Elements headerRows;
	private Elements footerRows;
	private Elements bodyRows;
	
	public HtmlTable(Element table){
		if(table == null) return;
		if(table.tagName().compareToIgnoreCase("table") != 0) return;
		this.table = table;
		
		int max = 0;
		int curMax;
		
		//looking for table title/caption
		Elements caption = table.select("table > caption");
		if(caption == null || caption.size()==0) this.hasTitle = false;
		else{
			this.hasTitle = true;
			this.title = String.valueOf(caption.first().toString());	//TODO encode HTML
		}
		
		//looking for table header
		Elements thead = table.select("table > thead");
		if(thead == null || thead.size()==0) this.hasHeader = false;
		else{
			this.hasHeader = true;
			this.headerRows = thead.select("tr");
			this.headerRowCount = this.headerRows.size();
			curMax = getColumnMax(this.headerRows);
			if(max < curMax) max = curMax; 
		}
		
		//looking for table footer
		Elements tfoot = table.select("table > tfoot");
		if(tfoot == null || tfoot.size()==0) this.hasFooter = false;
		else{
			this.hasFooter = true;
			this.footerRows = tfoot.select("tr");
			this.footerRowCount = this.footerRows.size();
			curMax = getColumnMax(this.footerRows);
			if(max < curMax) max = curMax;
		}
		
		//looking for table tbody
		Elements tbody = table.select("table > tbody");
		if(tbody == null || tbody.size()==0){
			this.bodyRows = table.select("table > tr");
			this.bodyRowCount = this.bodyRows.size();
			curMax = getColumnMax(this.bodyRows);
			if(max < curMax) max = curMax;
		}
		else{
			this.bodyRows = tbody.select("tbody > tr");
			this.bodyRowCount = this.bodyRows.size();
			curMax = getColumnMax(this.bodyRows);
			if(max < curMax) max = curMax;
		}
		
		this.colCount = max;
		this.init();
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
		//TODO need to handle rowspan and colspan
		StringBuffer sb = new StringBuffer();
		Elements bodyRows = this.bodyRows;
		if(!this.hasHeader) bodyRows.remove(0);
		sb.append("<utbody>");
		for(Element tr : bodyRows){
			sb.append("<row>");
			Elements cells = tr.select("tr > td");
			if(cells != null && cells.size() > 0){
				for(Element cell : cells){
					sb.append(String.format("<entry>%s</entry>", cell.html()));
				}
			}
			sb.append("</row>");
		}
		sb.append("</utbody>");
		return sb.toString();
	}
	
	private String generateFooter(){
		//TODO need to handle rowspan and colspan
		if(!this.hasFooter) return "";
		
		StringBuffer sb = new StringBuffer();		
		sb.append("<utfoot>");
		for(Element tr : footerRows){
			sb.append("<row>");
			Elements cells = tr.select("tr > th");
			if(cells == null || cells.size() == 0) cells = tr.select("tr > td");
			if(cells != null && cells.size() > 0){
				for(Element cell : cells){
					sb.append(String.format("<entry>%s</entry>", cell.html()));
				}
			}
			sb.append("</row>");
		}
		sb.append("</utfoot>");
		return sb.toString();
	}
	
	private String generateHeader(){
		//TODO need to handle rowspan and colspan
		StringBuffer sb = new StringBuffer();		
		sb.append("<uthead>");
		if(this.hasHeader){
			for(Element tr : headerRows){
				sb.append("<row>");
				Elements cells = tr.select("tr > th");
				if(cells == null || cells.size() == 0) cells = tr.select("tr > td");
				if(cells != null && cells.size() > 0){
					for(Element cell : cells){
						sb.append(String.format("<entry>%s</entry>", cell.html()));
					}
				}
				sb.append("</row>");
			}
		}
		else{
			Element tr = this.bodyRows.first();
			Elements cells = tr.select("tr > td");
			sb.append("<row>");
			if(cells != null && cells.size() > 0){
				for(Element cell : cells){
					sb.append(String.format("<entry>%s</entry>", cell.html()));
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
			Elements TDs = tr.select("tr > td");
			if(TDs != null && TDs.size() > 0){
				curMax = TDs.size();
				if(max < curMax) max = curMax;
			}
		}
		return max;
	}
	
	public int[][] getBody(){return body;}
	
	public int getBodyRowCount(){return bodyRowCount;}
	
	public int getColCount(){ return colCount;}
	
	public int getFooterRowCount() { return this.footerRowCount; }
	
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
	
	public String getTitle(){
		if(this.title == null || this.title.isEmpty()) return "";
		return this.title;
	}
	
	public boolean hasTitle() { return this.hasTitle; }
	
	public boolean hasHeader() { return this.hasHeader; }
	
	public boolean hasFooter() { return this.hasFooter; }
	
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
	
	public void init(){
		header = new int[headerRowCount][colCount];
		body = new int[bodyRowCount][colCount];
	}
	
}
