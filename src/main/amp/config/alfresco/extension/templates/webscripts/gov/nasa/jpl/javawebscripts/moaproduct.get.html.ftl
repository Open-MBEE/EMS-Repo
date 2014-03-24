<!DOCTYPE html>
<html>
  <head>
	<meta http-equiv="X-UA-Compatible" content="IE=edge;chrome=1" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>View Editor: ${title}</title>
    <link rel="stylesheet" href="${url.context}/scripts/vieweditor/vendor/css/bootstrap.min.css" media="screen">
    <link href="${url.context}/scripts/vieweditor/styles/jquery.tocify.css" rel="stylesheet" media="screen">
    <link href="${url.context}/scripts/vieweditor/styles/styles.css" rel="stylesheet" media="screen">
    <link href="${url.context}/scripts/vieweditor/styles/print.css" rel="stylesheet" media="print">
    <link href="${url.context}/scripts/vieweditor/styles/fonts.css" rel="stylesheet">
    <link href="${url.context}/scripts/vieweditor/vendor/css/whhg.css" rel="stylesheet" >
    <link href="${url.context}/scripts/vieweditor/styles/section-numbering.css" rel="stylesheet">
    <link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>
  
<script type="text/javascript">
var pageData = { viewHierarchy: ${res},  baseUrl: "${url.context}/service" };
</script>

</head>

  <body class="{{ meta.pageName }} {{ settings.currentWorkspace }}">
<div id="main"></div>

    <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
    	<div class="navbar-header">
    		<a class="navbar-brand" href="/share/page/site/${siteName}/dashboard">${siteTitle}</a>
    	</div>
    	<ul class="nav navbar-nav">
    	<li class="active"><a href="#">${title}</a></li>
        <li class="dropdown" id="firstDropdown">
        	<a href="#" class="dropdown-toggle" data-toggle="dropdown">Goto <b class="caret"></b></a>
        	<ul class="dropdown-menu">
        		<li><a href="${url.context}/service/ve/configurations/${siteName}">DocWeb</a></li>
        	<#if siteName == 'europa'>
        		<li><a href="${url.context}/service/ve/index/${siteName}">Document List</a></li>
        		<li><a href="${url.context}/service/ve/documents/${siteName}">In-Work Document List</a></li>
        	<#else>
        		<li><a href="${url.context}/service/ve/documents/${siteName}">Document List</a></li>
        	</#if>	
        		<li><a href="/share/page/site/${siteName}/dashboard">Dashboard</a></li>
   			</ul>
   		</li>
   		<li class="dropdown">
   			<a href="#" class="dropdown-toggle" data-toggle="dropdown">Other Sites <b class="caret"></b></a>
   			<ul class="dropdown-menu" id="otherSites">
   			
   			</ul>
   		</li>
   	  </ul>

      <div class="pull-right">
        <img class="europa-icon" src="${url.context}/scripts/vieweditor/images/europa-icon.png" />
      </div>

      <ul class="nav navbar-nav pull-right">
      	<li><a href="/share/page/site/ems-training/dashboard">Support</a></li>
        <li><a href="#" class="submit-logout">logout</a></li>
      </ul>
    </nav>
   

<script id="template" type="text/mustache">

    <div id="ie-alert" class="alert alert-warning alert-dismissable ie_warning">
      <button type="button" class="close no-print" proxy-click="hideIEMessage" aria-hidden="true">&times;</button>
      <span class="message no-print">Internet Explorer is not officially supported by View Editor.  Please use Firefox.</span>
    </div>

    <div id="top-alert" class="alert alert-danger alert-dismissable" style="display:none">
      <button type="button" class="close no-print" proxy-click="hideErrorMessage" aria-hidden="true">&times;</button>
      <span class="message no-print"></span>
    </div>

    <div class="wrapper">
      <div class="row split-view">


  <div class="col-xs-3">
    <div class="toggled-inspectors inspectors affix page  no-print" style="height:80%;">

      <select class="form-control" value="{{ currentInspector }}">
        <option value="document-info">Table of Contents</option>
        <!-- <option value="history">History</option> -->
        <!-- <option value="references">References</option> -->
        <option value="transclusionList">Cross reference</option>
        <option value="export">Model Versions</option>
      </select>

      <div id="document-info" class="inspector" style="height:100%;">
        <h3>Document info</h3>
<!--         <dl>
          <dt>Author</dt><dd>Chris Delp</dd>
          <dt>Last modified</dt><dd>8/14/13 2:04pm</dd>
        </dl>
 -->    
        <div id="toc" style="height:100%;"></div>
      </div>

      <div id="transclusionList" class="inspector" style="height:100%;">
        <h3>Elements</h3>
        <input class="form-control" type="text" placeholder="search" id="search" value="{{transcludableQuery}}" />
        <style id="search_style"></style>
        <div class="transclusionList" style="display:block; height:80%; overflow-y: auto">
          <div class="items">
            {{#viewTree.elements}}
              {{#name}}
              <div class="body searchable" data-search-index="{{searchIndex}}">
                <div class="qualifiedName" title="{{qualifiedName}}">{{qualifiedNameParentPreview}}</div>
                <div proxy-click="transclusionClick" proxy-mousedown="transclusionDown" proxy-mouseup="transclusionUp" data-trans-id={{mdid}} class="transcludable name">{{name}}</div>
                {{#documentation}}
                  <div proxy-click="transclusionClick" proxy-mousedown="transclusionDown" proxy-mouseup="transclusionUp" data-trans-id={{mdid}} class="transcludable documentation">{{documentationPreview}}</div>
                {{/documentation}}
                {{#value}}
                  <div proxy-click="transclusionClick" proxy-mousedown="transclusionDown" proxy-mouseup="transclusionUp" data-trans-id={{mdid}} class="transcludable dvalue">{{value}}</div>
                {{/value}}
              </div>
              {{/name}}
            {{/viewTree.elements}}
          </div>
        </div>
      </div>

      <div id="history" class="inspector" style="height:100%;">
        <h3>History</h3>
        <ul class="list-unstyled">
          <li>v1 &mdash; Chris Delp</li>
        </ul>
      </div>

<!--       <div id="references" class="inspector">
        <h3>References</h3>
        <ul>
          {{#viewHierarchy.elements}}
          <li>
             {{ name }}
          </li>
          {{/viewHierarchy.elements}}
        </ul>
      </div>
 -->
      <div id="export" class="inspector" style="height:100%;">
        <h3>Export</h3>
        <ul class="list-unstyled">
          <li><button type="button" class="btn btn-default" proxy-click="print">Print PDF</button></li>         
          <li><button type="button" class="btn btn-default" proxy-click="printPreview">Print Preview</button></li>
          {{^viewTree.snapshot}}
          <li><button type="button" class="btn btn-default" proxy-click="snapshot:{{(viewTree.id)}}">Mark Model Version </button></li>   
          {{/viewTree.snapshot}}       
        </ul>

        <ul class="list-unstyled" style="display:block; height:50%; overflow-y: auto">
        {{#viewTree.snapshots}}
          <li><a href="{{ url }}" target="_blank">{{ formattedDate }} ({{ creator }})  {{tag}}</a></li>
        {{/viewTree.snapshots}}
        </ul>
      </div>

    </div>
  </div>
  
<div class="col-xs-9">

<!-- Modal -->
<div class="modal fade" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
  <div class="modal-dialog" style="width:1024px">
    <div class="modal-content">
      <div class="modal-body">
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

  <div id="the-document">
    <div id="printTOC" class="print-only">
      Table of Contents
      <ul>
      {{#viewTree.orderedChildren}}
        {{^viewData.noSection}}
          <li style="margin-left: {{(depth*20)}}px;">{{ name }}</li>
        {{/viewData.noSection}}
      {{/viewTree.orderedChildren}}
      </ul>
      </div>
    <button type="button" id="saveAll" class="btn btn-primary no-print">Save all</button>
    {{#viewTree.orderedChildren}}
      {{^viewData.noSection}}
            <a name="{{id}}" style="display: block; position:relative; top:-60px; "></a>
            {{#(depth == 0) }}
              {{{("<h1 class='numbered-header'><span class='"+class+" section-header' data-property='name' data-section-id='" + id + "'>" +  name + "</span></h1>" )}}}
            {{/(depth == 0) }}
            {{^(depth == 0) }}
              {{{("<h"+ (depth+1) + " class='numbered-header "+class+"'><span class='section-header' data-section-id='" + id + "'><span class='editable' data-property='name' data-mdid='" + id + "'>" +  name + "</span></span></h"+ (depth+1) + ">" )}}}
            {{/(depth == 0) }}
           
            <div class="no-print author {{ class }}">Edited by <span class="author-name" data-mdid="{{id}}">{{ viewData.author }}</span></div>
            <div class="no-print modified {{ class }}" data-mdid="{{id}}">{{( viewData.modifiedFormatted )}}</div>
             <a href="${'#'}{{id}}" title="permalink" class="no-print"><i class='glyphicon glyphicon-link'></i>&nbsp;</a>
      {{/viewData.noSection}}
      <div class="page-sections {{ class }}">
        
        {{^(depth == 0) }}
          <div class="section-wrapper">
            
            <div class="section-actions pull-right btn-group no-print">
              {{^viewTree.snapshot}}
                <button type="button" class="btn btn-primary btn-sm" proxy-click="toggleComments:comments-{{id}}">comments ({{( viewData.comments.length )}})</button>
                {{#viewData.editable}}
                  {{^editing}}
                    <button type="button" href="#" class="btn btn-primary btn-sm" proxy-click="editSection:{{ id }}">edit</button>
                  {{/editing}}
                {{/viewData.editable}}
              {{/viewTree.snapshot}}
            </div>
            {{#editing}}

            <div class="toolbar page" data-role="editor-toolbar" data-target="#section{{ id }}">
              <div class="btn-group">
                  <a class="btn btn-default" data-edit="bold" title="Bold (Ctrl/Cmd+B)"><b>b</b></a>
                  <a class="btn btn-default" data-edit="italic" title="Italic (Ctrl/Cmd+I)"><i>i</i></a>
                  <a class="btn btn-default" data-edit="underline" title="Underline (Ctrl/Cmd+u)"><span style="text-decoration:underline">u</span></a>
                  <a class="btn btn-default" data-edit="strikethrough" title="Strike through"><span style="text-decoration:line-through">u</span></a>
                  <a class="btn btn-default" data-edit="superscript" title="Superscript">x<sup>y</sup></a>
                  <a class="btn btn-default" data-edit="subscript" title="Subscript">x<sub>y</sub></a>
                  <div class="btn-group">
                  <a class="btn btn-default dropdown-toggle" data-toggle="dropdown" title="Font Size">
                    <i class="glyphicon glyphicon-text-height"></i>&nbsp;<b class="caret"></b></a>
                    <ul class="dropdown-menu">
                      <li><a data-edit="fontSize 7"><font size="7">Biggest</font></a></li>
                      <li><a data-edit="fontSize 6"><font size="6">Bigger</font></a></li>
                      <li><a data-edit="fontSize 5"><font size="5">Big</font></a></li>
                      <li><a data-edit="fontSize 4"><font size="4">Larger</font></a></li>
                      <li><a data-edit="fontSize 3"><font size="3">Large</font></a></li>
                      <li><a data-edit="fontSize 2"><font size="2">Normal</font></a></li>
                      <li><a data-edit="fontSize 1"><font size="1">Small</font></a></li>
                    </ul>
                </div>
                  <div class="btn-group">
                    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" title="Background color">
                      <i class="glyphicon icon-ink">&nbsp;</i>
                      <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu">
                      <li><a data-edit="backcolor red"><i class="icon-tint"></i>Red</a></li>
                      <li><a data-edit="backcolor limegreen"><i class="icon-tint"></i>Green</a></li>
                      <li><a data-edit="backcolor blue"><i class="icon-tint"></i>Blue</a></li>
                      <li><a data-edit="backcolor black"><i class="icon-tint"></i>Black</a></li>
                      <li><a data-edit="backcolor transparent"><i class="icon-tint"></i>None</a></li>
                    </ul>
                  </div>
                  <div class="btn-group">
                    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" title="Font color">
                      <i class="glyphicon icon-tint">&nbsp;</i>
                      <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu">
                      <li><a data-edit="foreColor red"><i class="icon-tint"></i>Red</a></li>
                      <li><a data-edit="foreColor limegreen"><i class="icon-tint"></i>Green</a></li>
                      <li><a data-edit="foreColor blue"><i class="icon-tint"></i>Blue</a></li>
                      <li><a data-edit="foreColor black"><i class="icon-tint"></i>Black</a></li>
                      <li><a data-edit="foreColor transparent"><i class="icon-tint"></i>None</a></li>
                    </ul>
                  </div>
                  <div class="btn-group">
                    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
                      &forall;
                      <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu">
                      <table>
                        <tr>
                          <td><a data-edit="insertHTML &forall; ">&forall;</a></td>
                          <td><a data-edit="insertHTML &part; ">&part;</a></td>
                          <td><a data-edit="insertHTML &empty; ">&empty;</a></td>
                          <td><a data-edit="insertHTML &nabla; ">&nabla;</a></td>
                          <td><a data-edit="insertHTML &isin; ">&isin;</a></td>
                          <td><a data-edit="insertHTML &notin; ">&notin;</a></td>
                          <td><a data-edit="insertHTML &ni; ">&ni;</a></td>
                          <td><a data-edit="insertHTML &prod; ">&prod;</a></td>
                          <td><a data-edit="insertHTML &sum; ">&sum;</a></td>  
                          <td><a data-edit="insertHTML &minus; ">&minus;</a></td></tr><tr>
                          <td><a data-edit="insertHTML &lowast; ">&lowast;</a></td>
                          <td><a data-edit="insertHTML &radic; ">&radic;</a></td>
                          <td><a data-edit="insertHTML &prop; ">&prop;</a></td>
                          <td><a data-edit="insertHTML &infin; ">&infin;</a></td>
                          <td><a data-edit="insertHTML &ang; ">&ang;</a></td>
                          <td><a data-edit="insertHTML &and; ">&and;</a></td>
                          <td><a data-edit="insertHTML &or; ">&or;</a></td>
                          <td><a data-edit="insertHTML &cap; ">&cap;</a></td>
                          <td><a data-edit="insertHTML &cup; ">&cup;</a></td>
                          <td><a data-edit="insertHTML &int; ">&int;</a></td></tr><tr>
                          <td><a data-edit="insertHTML &sim; ">&sim;</a></td>
                          <td><a data-edit="insertHTML &cong; ">&cong;</a></td>
                          <td><a data-edit="insertHTML &asymp; ">&asymp;</a></td>
                          <td><a data-edit="insertHTML &ne; ">&ne;</a></td>
                          <td><a data-edit="insertHTML &equiv; ">&equiv;</a></td>
                          <td><a data-edit="insertHTML &le; ">&le;</a></td>
                          <td><a data-edit="insertHTML &ge; ">&ge;</a></td>
                          <td><a data-edit="insertHTML &sub; ">&sub;</a></td>
                          <td><a data-edit="insertHTML &sup; ">&sup;</a></td>
                          <td><a data-edit="insertHTML &nsub; ">&nsub;</a></td></tr><tr>
                          <td><a data-edit="insertHTML &sube; ">&sube;</a></td>
                          <td><a data-edit="insertHTML &supe; ">&supe;</a></td>
                          <td><a data-edit="insertHTML &oplus; ">&oplus;</a></td>
                          <td><a data-edit="insertHTML &otimes; ">&otimes;</a></td>
                          <td><a data-edit="insertHTML &perp; ">&perp;</a></td>
                          <td><a data-edit="insertHTML &sdot; ">&sdot;</a></td>
                          <td><a data-edit="insertHTML &Gamma; ">&Gamma;</a></td>
                          <td><a data-edit="insertHTML &Delta; ">&Delta;</a></td>
                          <td><a data-edit="insertHTML &Theta; ">&Theta;</a></td>
                          <td><a data-edit="insertHTML &Lambda; ">&Lambda;</a></td></tr><tr>
                          <td><a data-edit="insertHTML &Xi; ">&Xi;</a></td>
                          <td><a data-edit="insertHTML &Pi; ">&Pi;</a></td>
                          <td><a data-edit="insertHTML &Rho; ">&Rho;</a></td>
                          <td><a data-edit="insertHTML &Sigma; ">&Sigma;</a></td>
                          <td><a data-edit="insertHTML &Phi; ">&Phi;</a></td>
                          <td><a data-edit="insertHTML &Psi; ">&Psi;</a></td>
                          <td><a data-edit="insertHTML &Omega; ">&Omega;</a></td>
                          <td><a data-edit="insertHTML &alpha; ">&alpha;</a></td>
                          <td><a data-edit="insertHTML &beta; ">&beta;</a></td>
                          <td><a data-edit="insertHTML &gamma; ">&gamma;</a></td></tr><tr>
                          <td><a data-edit="insertHTML &delta; ">&delta;</a></td>
                          <td><a data-edit="insertHTML &epsilon; ">&epsilon;</a></td>
                          <td><a data-edit="insertHTML &zeta; ">&zeta;</a></td>
                          <td><a data-edit="insertHTML &eta; ">&eta;</a></td>
                          <td><a data-edit="insertHTML &theta; ">&theta;</a></td>
                          <td><a data-edit="insertHTML &iota; ">&iota;</a></td>
                          <td><a data-edit="insertHTML &kappa; ">&kappa;</a></td>
                          <td><a data-edit="insertHTML &lambda; ">&lambda;</a></td>
                          <td><a data-edit="insertHTML &mu; ">&mu;</a></td>
                          <td><a data-edit="insertHTML &nu; ">&nu;</a></td></tr><tr>
                          <td><a data-edit="insertHTML &xi; ">&xi;</a></td>
                          <td><a data-edit="insertHTML &omicron; ">&omicron;</a></td>
                          <td><a data-edit="insertHTML &pi; ">&pi;</a></td>
                          <td><a data-edit="insertHTML &rho; ">&rho;</a></td>
                          <td><a data-edit="insertHTML &sigmaf; ">&sigmaf;</a></td>
                          <td><a data-edit="insertHTML &sigma; ">&sigma;</a></td>
                          <td><a data-edit="insertHTML &tau; ">&tau;</a></td>
                          <td><a data-edit="insertHTML &upsilon; ">&upsilon;</a></td>
                          <td><a data-edit="insertHTML &phi; ">&phi;</a></td>
                          <td><a data-edit="insertHTML &chi; ">&chi;</a></td></tr><tr>
                          <td><a data-edit="insertHTML &psi; ">&psi;</a></td>
                          <td><a data-edit="insertHTML &omega; ">&omega;</a></td>
                          <td><a data-edit="insertHTML &thetasym; ">&thetasym;</a></td>
                          <td><a data-edit="insertHTML &upsih; ">&upsih;</a></td>
                          <td><a data-edit="insertHTML &piv; ">&piv;</a></td>
                          <td style="border-style:none;"></td>
                          <td style="border-style:none;"></td>
                          <td style="border-style:none;"></td>
                          <td style="border-style:none;"></td>
                          <td style="border-style:none;"></td>
                        </tr>
                      </table>
                    </ul>
                  </div>
              </div>
              
              <div class="btn-group">
                <a class="btn btn-default" data-edit="justifyleft" title="Align Left (Ctrl/Cmd+L)"><i class="glyphicon glyphicon-align-left"></i>&nbsp;</a>
                <a class="btn btn-default" data-edit="justifycenter" title="Center (Ctrl/Cmd+E)"><i class="glyphicon glyphicon-align-center"></i>&nbsp;</a>
                <a class="btn btn-default" data-edit="justifyright" title="Align Right (Ctrl/Cmd+R)"><i class="glyphicon glyphicon-align-right"></i>&nbsp;</a>
                <a class="btn btn-default" data-edit="justifyfull" title="Justify (Ctrl/Cmd+J)"><i class="glyphicon glyphicon-align-justify"></i>&nbsp;</a>
              </div>
              <div class="btn-group">
                <a class="btn dropdown-toggle btn-default" data-toggle="dropdown" title="Hyperlink"><i class="glyphicon glyphicon-link"></i>&nbsp;</a>
                <div class="dropdown-menu input-append">
                  <input class="span2" placeholder="URL" type="text" data-edit="createLink"/>
                  <button class="btn" type="button">Add</button>
                </div>
                <a class="btn btn-default" data-edit="unlink" title="Remove Hyperlink"><i class="glyphicon icon-brokenlink"></i>&nbsp;</a>
              </div>

              <br/>
              <div class="btn-group">
                  <a class="btn btn-default" data-edit="insertunorderedlist" title="Bullet list">&bull;</a>
                  <a class="btn btn-default" data-edit="insertorderedlist" title="Bullet list">1.</a>
                  <a class="btn btn-default" data-edit="indent" title="Indent (Tab)">&rarr;</a>
                  <a class="btn btn-default" data-edit="outdent" title="Reduct Indent (Shift-Tab)">&larr;</a>
              </div>


              <a class="btn btn-default" data-edit="undo" title="Undo"><i class="glyphicon icon-undo"></i>&nbsp;</a>
              <a class="btn btn-default" data-edit="redo" title="Redo"><i class="glyphicon icon-repeat"></i>&nbsp;</a>
              <div class="btn-group">
                <a class="btn dropdown-toggle btn-default " data-toggle="dropdown" title="Hyperlink"><i class="glyphicon icon-fullborders"></i>&nbsp;</a>
                <div class="dropdown-menu input-append">
                  <input class="span2 tablerows" placeholder="Rows" type="text"/>
                  <input class="span2 tablecols" placeholder="Cols" type="text"/>
                  <button class="btn addtable" type="button">Add</button>
                </div>               
              </div>
              <div class="btn-group">
                <a class="btn btn-default" title="Insert picture (or just drag &amp; drop)" id="pictureBtn{{id}}">img</a>
                <input type="file" data-role="magic-overlay" data-target="#pictureBtn{{id}}" data-edit="insertImage">
                <button type="button" class="btn btn-default requires-selection" title="Insert SVG" onclick="insertSvg('{{id}}');">svg</button>
              </div>
              <a class="btn btn-default dummyButton" style="visibility:hidden" data-edit="insertHTML &nbsp;" title="dumb"></a>


              <div class="btn-group pull-right">
                <button type="button" class="btn btn-default" proxy-click="cancelEditing">Cancel</button>
                <button type="button" class="btn btn-primary saveSection" proxy-click="saveSection:{{ id }}">Save changes</button>
              </div>
            </div>
            <div id="section{{ id }}" class="section page editing" data-section-id="{{ id }}" contenteditable="false" proxy-dblclick="sectionDoubleClick">
              {{{ content }}}
            </div>
            {{/editing}}
            {{^editing}}
            <div class="section page" data-section-id="{{ id }}">
              {{{ content }}}
            </div>
            {{/editing}}
             <div class="comments" id="comments-{{id}}" style="display:none">
              <ul class="list-group">
                {{#viewData.comments}}
                    <li class="comment list-group-item">
                      {{{ body }}}
                      <div class="comment-info"><small>{{ author }}, {{ lastModified }}<small></div>
                    </li>
                {{/viewData.comments}}
                <li class="list-group-item">
                  <div class="comment-form">
                    <br/>
                    <!-- <textarea class="form-control" value="{{ newComment }}"></textarea> -->
                    <div class="btn-group" data-role="editor-toolbar" data-target="#comment-form-{{id}}">
                      <a class="btn btn-default" data-edit="bold" title="" data-original-title="Bold (Ctrl/Cmd+B)"><b>b</b></a>
                      <a class="btn btn-default" data-edit="italic" title="" data-original-title="Italic (Ctrl/Cmd+I)"><i>i</i></a>
                    </div>

                    <div id="comment-form-{{id}}" class="comment-editor form-control" contenteditable="true">
                    </div>
                    <br/>
                    <button type="button" class="btn btn-primary add-comment" proxy-click="addComment:{{id}}">Add comment</button>
                  </div>
                </li>
              </ul>

            </div>
          </div>
        {{/(depth == 0) }}
      </div>


    {{/viewTree.orderedChildren}} 
  </div>
 </div> 




</div>





    </div>

    
    
    
    
    
    
    
  </script><script src="${url.context}/scripts/vieweditor/vendor/jquery.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery-ui.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.hotkeys.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/bootstrap-wysiwyg.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.tocify.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/underscore.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/moment.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/bootstrap.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/svgedit/embedapi.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/css_browser_selector.js"></script>
<script type="text/javascript" src="${url.context}/scripts/vieweditor/vendor/Ractive.js"></script>
<script type="text/javascript">
$(document).ready(function() {
	$('a.submit-logout').click(function() {
		window.location.replace('${url.context}/service/logout/info?next=${url.full}');
	});
	$.getJSON('/alfresco/service/rest/sites').done(function(data) {
		var sites = {};
		for (var i = 0; i < data.length; i++) {
			var site = data[i];
			if (site.categories.length == 0)
				site.categories.push("Uncategorized");
			for (var j = 0; j < site.categories.length; j++) {
				var cat = site.categories[j];
				if (sites.hasOwnProperty(cat)) {
					sites[cat].push(site);
				} else {
					sites[cat] = [site];
				}
			}
		}
		var stuff = "";
		var keys = Object.keys(sites).sort();
		for (var i = 0; i < keys.length; i++) {
			var key = keys[i];
			stuff += '<li class="dropdown dropdown-submenu"><a href="#" class="dropdown-toggle" data-toggle="dropdown">' + key + '</a>';
			stuff += '<ul class="dropdown-menu">';
        	var ssites = sites[key].sort(function(a,b) {
        		if (a.title > b.title)
        			return 1;
        		if (a.title < b.title)
        			return -1;
        		return 0;
        	});
        
			for (var j = 0; j < ssites.length; j++) {
				stuff += '<li class="dropdown-submenu"><a href="#" class="dropdown-toggle" data-toggle="dropdown">' + ssites[j].title + '</a><ul class="dropdown-menu">';
				stuff += '<li><a href="/share/page/site/' + ssites[j].name + '/dashboard">Dashboard</a></li>';
				stuff += '<li><a href="/alfresco/service/ve/configurations/' + ssites[j].name + '">DocWeb</a></li>';
				stuff += '</ul></li>';
			}
        	stuff += '</ul></li>';
		};
		$('#otherSites').append(stuff);
		
	});
	$('ul.dropdown-menu [data-toggle=dropdown]').on('click', function(event) {
    // Avoid following the href location when clicking
    event.preventDefault(); 
    // Avoid having the menu to close when clicking
    event.stopPropagation(); 
    // If a menu is already open we close it
    //$('ul.dropdown-menu [data-toggle=dropdown]').parent().removeClass('open');
    // opening the one you clicked on
    $(this).parent().addClass('open');

    var menu = $(this).parent().find("ul");
    var menupos = menu.offset();
  
    if ((menupos.left + menu.width()) + 30 > $(window).width()) {
        var newpos = - menu.width();      
    } else {
        var newpos = $(this).parent().width();
    }
    menu.css({ left:newpos });

	});
});
</script>
<script type="text/javascript">var app = new Ractive({ el : "main", template : "#template", data : pageData });</script>
<script type="text/javascript">
var context = window;

// backend.js

// Provides handlers for:
//  saveView
//  saveComment

var absoluteUrl = function(relativeUrl) {
  return (app.data.baseUrl || '') + relativeUrl;
}

var ajaxWithHandlers = function(options, successMessage, errorMessage) {
  $.ajax(options)
    .done(function(data) {
    /* 
      if (data.indexOf("html") != -1) {
        alert("Not saved! You've been logged out, login in a new window first!");
          window.open("/alfresco/faces/jsp/login.jsp?_alfRedirect=/alfresco/service/ui/relogin");
      }
     */
      app.fire('message', 'success', successMessage); })
    .fail(function(e) { 
      if (e && e.status && e.status === 200) {
        if (e.responseText.indexOf("html") != -1) {
          alert("Not saved! You've been logged out, login in a new window first!");
          window.open("/alfresco/faces/jsp/login.jsp?_alfRedirect=/alfresco/service/ui/relogin");
        }
        // we got a 200 back, but json parsing might have failed
        return;
      } else {
        if(e && e.status && e.status === 401) {
          errorMessage += ": user does not have authorization to perform this action";
        }
        app.fire('message', 'error', errorMessage); 
        if (console && console.log) {
          console.log("ajax error:", e);
        }        
      }
    })
}

app.on('saveView', function(viewId, viewData) {
  var jsonData = JSON.stringify(viewData);
  console.log("Saving ", jsonData);
  //alfresco/service/javawebscripts/views/id/elements
  var url = absoluteUrl('/javawebscripts/views/' + viewId + '/elements');
  ajaxWithHandlers({ 
    type: "POST",
    url: url,
    data: jsonData,
    contentType: "application/json; charset=UTF-8"
  }, "Saved view", "Error saving view");
})

app.on('saveComment', function(evt, viewId, commentBody) {
  var url = absoluteUrl("/javawebscripts/views/" + viewId + "/elements");
  var jsonData = JSON.stringify({"elements": [{"id": "_comment_" + (new Date()).getTime(), "body": commentBody, "type": "Comment", "annotatedElements":[viewId]}]});
  console.log(jsonData);
  ajaxWithHandlers({ 
    type: "POST",
    url: url,
    data: jsonData,
    contentType: "application/json; charset=UTF-8"
  }, "Saved comment", "Error saving comment"); 
})

app.on('saveSnapshot', function(viewId, html) {
  var url = absoluteUrl('/ui/views/' + viewId + '/snapshot');
  ajaxWithHandlers({ 
    type: "POST",
    url: url,
    data: html,
    contentType: "application/json; charset=UTF-8",
    success: function(result){
      app.fire("snapshotSuccess", result);
    }
  }, "Saved Snapshot", "Error saving snapshot");
  
})

// comments.js

var selectBlank = function($el) {
  $el.html('&nbsp;');
  // execCommand('selectAll');
  $el.off('click');
}

app.placeholder = function($el, defaultText) {
 $el.html('<div class="placeholder">'+defaultText+'</div>').click(function() {
    selectBlank($el);
  });
}

app.on('toggleComments', function(evt, id) {
  var comments = $('#'+id);
  var showing = !comments.is(':visible');
  if (showing) {
    var commentField = comments.toggle().find('.comment-editor').wysiwyg();
    // app.placeholder(commentField, "Type your comment here");
    commentField.one("focus", function() {
      window.pageExitManager.editorOpened();
    })
   
    selectBlank(commentField);
  } else {
    comments.hide();
  }
});

app.on('addComment', function(evt, mbid) {
  window.pageExitManager.editorClosed();
  var commentFieldId = "#comment-form-" + mbid;
  var commentField = $(commentFieldId);
  commentField.one("focus", function() {
      window.pageExitManager.editorOpened();
  }); 

  commentField.find('.placeholder').detach();
  var newCommentBody = commentField.cleanHtml();
  if (newCommentBody != "") {
    app.get(evt.keypath+".viewData.comments").push({ author : 'You', body : newCommentBody, lastModified : new Date()});

    app.fire('saveComment', null, mbid, newCommentBody);
  }
  app.placeholder(commentField, "Type your comment here");
});

// editor.js

app.getSelectedNode = function() {
  return document.selection ? document.selection.createRange().parentElement() : window.getSelection().anchorNode.parentNode;
}

window.pageExitManager = function() 
{
  var numOpenEditors = 0;

  var confirmOnPageExit = function (e) 
  {
      // If we haven't been passed the event get the window.event
      e = e || window.event;

      var message = 'There are ' + numOpenEditors + ' unsaved sections.';

      // For IE6-8 and Firefox prior to version 4
      if (e) 
      {
          e.returnValue = message;
      }

      // For Chrome, Safari, IE8+ and Opera 12+
      return message;
  };

  return {
    editorOpened : function() {
      numOpenEditors += 1;
      app.updateSaveAllButton(numOpenEditors);
      window.onbeforeunload = confirmOnPageExit;
    },
    editorClosed : function() {
      numOpenEditors -= 1;
      if(numOpenEditors < 0) {
        numOpenEditors = 0;
      }
      if(numOpenEditors === 0)
      {
        window.onbeforeunload = null;
      }
      app.updateSaveAllButton(numOpenEditors);
    }
  }
}();

app.updateSaveAllButton = function(n) {
  if(n === 0) {
    $("#saveAll").hide();
  } else {
    $("#saveAll").show();
    // When clicked, click all save section buttons
    $("#saveAll").click( function() {
      $(".saveSection").click(); 
      $(".add-comment").click();
    });
  }
}

app.transToText = function transToText(s) {
  var t = "[";
  var elem = $("[data-trans-id='"+s.attr("data-mdid")+"'].name");
  var name = s.attr("data-name");
  if(elem.length > 0) {
    name = $("[data-trans-id='"+s.attr("data-mdid")+"'].name").text();
  }
  t += '"' + name + '":';
  //var d = data[s.attr("mdid")];
  //if(d) t += '"' + d.name + '":';
  //else t += '"Element Not Found":';
  t += s.attr("data-type") + ":";
  t += s.attr("data-mdid") + "]";
  return t;
}

app.replaceSpanWithBracket = function(section) {

  section.find(".transclusion").each(function(){
      var text = app.transToText($(this));
      $(this).replaceWith(text);
  }); 

}

var savedSel;


function _saveSelection() {
  //console.log("Start");
    if (window.getSelection) {
        sel = window.getSelection();
        if (sel.getRangeAt && sel.rangeCount) {
            var ranges = [];
            for (var i = 0, len = sel.rangeCount; i < len; ++i) {
                ranges.push(sel.getRangeAt(i));
            }
                        //console.log("End");
            return ranges;

        }
    } else if (document.selection && document.selection.createRange) {
                  console.log("End");
        return document.selection.createRange();
    }
    //console.log("End");
    return null;
}

function _restoreSelection(savedSel) {
    if (savedSel) {
        if (window.getSelection) {
            sel = window.getSelection();
            sel.removeAllRanges();
            for (var i = 0, len = savedSel.length; i < len; ++i) {
                sel.addRange(savedSel[i]);
            }
        } else if (document.selection && savedSel.select) {
            savedSel.select();
        }
    }
}

app.getSavedSelection = function()
{
  return savedSel;
}

app.saveSelection = function()
{
  //console.log("SAVE");
  savedSel = _saveSelection();
}

app.restoreSelection = function()
{
  _restoreSelection(savedSel);
}


app.on('editSection', function(e, sectionId) {
  window.pageExitManager.editorOpened();

  e.original.preventDefault();
  // TODO turn editing off for all other sections?
  app.set(e.keypath+'.editing', true);
  var section = $("[data-section-id='" + sectionId + "']");
  var toolbar =$('[data-role=editor-toolbar][data-target="#section' + sectionId + '"]');

  section.find(".reference.editable").attr('contenteditable', true);

  var sectionHeader = section.filter('.section-header');
  sectionHeader.data('original-content', sectionHeader.html());
  // bind toolbar properly, no toolbar for section name
  section.filter('.section.page').wysiwyg({toolbarSelector: '[data-role=editor-toolbar][data-target="#section' + sectionId + '"]'});
  section.filter('span').wysiwyg({toolbarSelector : '#no-toolbar'});
  
  // We will set the individual editable elements to contenteditable true.  If this root container is true than
  // it will prevent sub elements from getting key events.
  section.filter('.section.page').attr("contenteditable", false);

  app.replaceSpanWithBracket(section);

  app.set('currentInspector', 'transclusionList');


  toolbar.find(".requires-selection").addClass("disabled");
  var sectionPage = section.filter(".section.page");
  // On focus, enable the button
  section.find("[data-mdid][data-property='documentation']").on('focus', function(arg)
  {
    //console.log("Start Section focus")
    toolbar.find(".requires-selection").removeClass("disabled");
    // console.log("end Section focus")
  })
  // On blur disable the button unless the object we clicked on was the button itself
  // This code can only find the button in FF and Chrome
  section.find("[data-mdid][data-property='documentation']").on('blur', function(arg)
  {
    //console.log("Start Section blur");
    var target = arg.relatedTarget; // Chrome
    // Guess what, relatedTarget doesn't work on FF, if this is the case, look for originalEvent
    // Safari fails here, IE Unknown
    if(target === null && arg.originalEvent !== null)
    {
      target = arg.originalEvent.explicitOriginalTarget; // FF
    }
    var clickedButton = $(target).filter(".requires-selection");
    toolbar.find(".requires-selection").not(clickedButton).addClass("disabled");
    //console.log("End Section blur");
  })


  // Wrap content inisde of p tags if it isn't already.  Without this, Chrome will create new DIVs when 
  // enter is pressed and give them attributes from the parent div, including mdid
  var unwrapped = section.find(".editable.reference.doc").not(".blank").filter(function() {
    return $(this).find('p:first-child').length === 0;
  });
  unwrapped.wrapInner("<p class='pwrapper'></p>");

  section.find("p").addClass("pwrapper");
  
  section.on('keyup paste click',function(evt) {
    app.saveSelection();
  });

  // TODO: turn off this handler?
  // Update other references to an element within this view on change
  section.find("[data-mdid][data-property]").on('keyup paste blur', function(evt) 
  {
      var editedElement = evt.target;//app.getSelectedNode();
      var $el = $(editedElement);
      var mdid = $el.attr('data-mdid');
      var property = $el.attr('data-property');
      var newValue =  $el.html();
      section.find("[data-mdid='"+mdid+"'][data-property='"+property+"']").not($el).html(newValue);
  });

  // handle placeholder text
  // TODO remove this listener on cancel or save
  section.find(".editable.reference.blank").on('keyup paste blur mousedown', function(evt) 
  {
      var editedElement = evt.target;//app.getSelectedNode();
      var $el = $(editedElement);
      if($el.is(".blank.doc")) {
        console.log("Doc down");
        $el.html("<p class='pwrapper'>&nbsp;</p>");
        $el.removeClass('blank');

        var range = document.createRange();//Create a range (a range is a like the selection but invisible)
        range.setStart($el.find("p").get(0),0);
        range.collapse(true);
        var selection = window.getSelection();//get the selection object (allows you to change selection)
        selection.removeAllRanges();//remove any selections already made
        selection.addRange(range);//make the range you have just created the visible selection
        $el.click();

      } else if($el.is('.blank')) {
        console.log("Other down");
        $el.html("&nbsp;");
        $el.removeClass('blank');
        
        var range = document.createRange();
        console.log("First", $el.first().get(0).firstChild);
        range.setStart($el.first().get(0).firstChild,0);
        range.collapse(true);
        var selection = window.getSelection();//get the selection object (allows you to change selection)
        selection.removeAllRanges();//remove any selections already made
        selection.addRange(range);
        $el.click();
      }
  });

  var templateTable = function(rows, cols)
  {
    var result = "<table class='manualtable'>";
    for(var x=0; x < rows; x++)
    {
      result += "<tr>";
      for(var y = 0; y < cols; y++)
      {
        result += "<td>&nbsp;</td>";
      }
      result += "</tr>"
    }
    result += "</table>";
    return result;
  }

  $(".addtable").click( function() {
    //document.execCommand('enableInlineTableEditing', true);
    var rows = parseInt($(".tablerows").val());
    var cols = parseInt($(".tablecols").val());
    var dummyButton = $('[data-role=editor-toolbar][data-target="#section' + sectionId + '"]').find(".dummyButton");
    dummyButton.click();
    document.execCommand("insertHTML", false, templateTable(rows, cols));
  })

  // Required so add link pop dialog doesn't disapear when user clicks in text box
  $('.dropdown-menu input')
        .click(function() {return false;})
        .change(function () {
          //console.log($(this).parent('.dropdown-menu').siblings('.dropdown-toggle').len())
          //$(this).parent('.dropdown-menu').siblings('.dropdown-toggle').dropdown('toggle');
        })
        .keydown('esc', function () {this.value='';$(this).change();});
  // make sneaky overlay for image uploads
  $('[data-role=magic-overlay]').each(function () {
    var overlay = $(this), target = $(overlay.data('target')); 
    overlay.css('opacity', 0).css('position', 'absolute').offset(target.offset()).width(target.outerWidth()).height(target.outerHeight());
  });

$('[data-toggle=dropdown]').dropdown();
  app.fire('initializeSvgEditor', section.filter('.page'));

})

app.on('cancelEditing', function(e) {
  window.pageExitManager.editorClosed();
  e.original.preventDefault();
  //var section = $("[data-section-id='" + sectionId + "']");
  var sectionId = app.get(e.keypath+'.id');
  app.set(e.keypath+'.editing', false);
  // console.log("canceling", sectionId);
  var $sectionHeader = $('.section-header[data-section-id="'+sectionId+'"]');
  // console.log("canceled editing for section header", $sectionHeader);
  $sectionHeader.html($sectionHeader.data('original-content'));
  $sectionHeader.attr('contenteditable', false);

  //section.find(".reference.editable").attr('contenteditable', false);

  //section.find("p").removeClass("pwrapper");
  // app.set(e.keypath+'.content', app.get(e.keypath+'.previousContent'));
  // console.log("canceled", app.get(e.keypath));
})

app.spanContentToBracket = function(content) {
  var contentDom = $('<div>' + content + '</div>');
  contentDom.find(".transclusion").each(function(i){
    var dom = $(this);
    var t = app.transToText(dom);
    dom.html(t);
  });
  return contentDom.html();
}

app.spanContentToValue = function(content, vtree, depth) {
  var contentDom = $('<div>' + content + '</div>');
  if(depth === undefined) {
    depth = 0;
  }
  contentDom.find(".transclusion").each(function(i){

      var dom = $(this);
      var id = dom.attr("data-mdid");
      var t = dom.attr("data-type");

      var elem = _.filter(vtree.elements, function(curElem) {
        return curElem.mdid === id;
      })
      elem = elem[0];
      if(elem !== null && elem !== undefined) {
        var innerVal = "Not Found";
        if(t === "name"){
          innerVal = elem.name;
        } else if (t === "documentation") {
          innerVal = elem.documentation;//"ASDFASF";//elem.documentation.text();
        } else if (t === "value") {
          innerVal = elem.value;
        }

        // TODO:  If the transcluded element has html in it, then we can't just include it in a span. 
        // Using a div or p tag for transcluded elements creates its own set of joys because jquery
        // has its own ideas about how things should change when these elements are nested.          
        if(depth <= 3) {
          innerVal = app.spanContentToValue(innerVal, vtree, depth + 1);
        } else {
          innerVal = " ERROR_MAX_NESTED_REF_DEPTH "
        }
        var docDom = $("<div>" + innerVal + "</div>");
        //console.log(docDom.find(".transclusion"))
        innerVal = docDom.text();

        // Set the hovertext to display the fully qualified name if it exists
        var hoverText = "[" + t + "]";
        if(elem.qualifiedName)
        {
          hoverText = elem.qualifiedName + " " + hoverText;
        }
        dom.attr("title", hoverText);

        dom.html(innerVal);
      } else {
        dom.addClass("missing-trans-ref");
      }
     
  });
  return contentDom.html();
}

app.replaceBracketWithSpan = function(content)
{
  content = content.replace(/(\[\"([^"]*)\":([^\s:]*):([^\s:]*)\])/g,"<span class='transclusion' data-mdid='$4' data-type='$3' data-name='$2'>$1</span>");
  return app.spanContentToValue(content, app.get("viewTree"));//app.spanContentToValue(content);
}

app.on('saveSection', function(e, sectionId) {
  window.pageExitManager.editorClosed();

  var section = $("[data-section-id='" + sectionId + "']");
  section.find(".reference.editable").attr('contenteditable', false);

  //section.find("p").removeClass("pwrapper");

  e.original.preventDefault();

  $('.modified[data-mdid="' + sectionId+ '"]').text(app.formatDate(new Date()));
  $('.author-name[data-mdid="' + sectionId+ '"]').text("You");

  //console.log("savesection", section);
  app.set(e.keypath+'.name', section.filter(".section-header").html());


  // update viewTree with changes
  var viewTree = app.get("viewTree");
  var elements = app.editableElements(section);
  _.each(elements, function(e) {
    _.each(viewTree.elements, function(cure) {
      if(cure.mdid === e.mdid)
      {
        if(e.hasOwnProperty("documentation"))
        { e.documentation = e.documentation.replace(/^((<br>)|(<\/br>)|\s)*|((<br>)|(<\/br>)|\s)*$/gi,"");
          cure.documentation = app.replaceBracketWithSpan(e.documentation);
          var preview = app.generatePreviewDocumentation(cure.documentation);
          if(preview) {
            cure.documentationPreview = preview;
          }
          // update value in transclusion tab
          $('[data-trans-id="'+cure.mdid+'"].transcludable.documentation').html(cure.documentationPreview);
          // update other references to this element in document
          $("[data-mdid='"+cure.mdid+"'][data-property='documentation']").html(cure.documentation);
        }
        if(e.hasOwnProperty("name"))
        {
          e.name = e.name.replace(/^((<br>)|(<\/br>)|\s)*|((<br>)|(<\/br>)|\s)*$/gi,"");
          cure.name = app.replaceBracketWithSpan(e.name);
          // update value in transclusion tab
          $('[data-trans-id="'+cure.mdid+'"].transcludable.name').html(cure.name);
          // update other references to this element in document
          $("[data-mdid='"+cure.mdid+"'][data-property='name']").html(cure.name);
        }
        if(e.hasOwnProperty("value"))
        {
          e.value = e.value.replace(/^((<br>)|(<\/br>)|\s)*|((<br>)|(<\/br>)|\s)*$/gi,"");
          cure.value = app.replaceBracketWithSpan(e.value);
          // update value in transclusion tab
          $('[data-trans-id="'+cure.mdid+'"].transcludable.dvalue').html(cure.value);
          // update other references to this element in document
          $("[data-mdid='"+cure.mdid+"'][data-property='value']").html(cure.value);
        }
      }
    });
  })

  var content = section.filter(".section").html();

  //content = app.replaceBracketWithSpan(content);
 
  
  app.set(e.keypath+'.content', content);//section.filter(".section").html());  
  app.set(e.keypath+'.editing', false);
  app.set(viewTree);

  // update all other transclusions 
  $(".transclusion").each(function(){
    var h = $(this)[0].outerHTML;
    h = app.spanContentToValue(h, viewTree);
    $(this).replaceWith(h);
      //var text = app.transToText($(this));
      //$(this).replaceWith(text);
  }); 

  //app.fire('saveSectionComplete', e, sectionId);
  //If data is saved before references are converted to dom elements, 
  //change realData saveSection to saveSectionComplete and fire event above
  //console.log("survived");
})

app.on('insertTable', function(e) {
  // TODO how do we initialize this ractive for existing content?
  var tableData = [
    ['Header 1', 'Header 2'],
    ['Value 1', 'Value 2']
  ];

  // var tableContent = '<table class="table table-bordered table-striped"><tr><td>your stuff here</td></tr></table>';
  document.execCommand('insertHTML', false, '<div id="tableTest" class="rich-table">table goes here</div>');
  var liveTable = app.createLiveTable($('#tableTest'), tableData);
  liveTable.set('editing', true);

})

app.on('insertReference', function() {
  document.execCommand('insertHTML', false, '<div id="referenceTest" class="rich-reference">reference goes here</div>');
  var liveReference = app.createLiveReference($('#referenceTest'), {}, app.get('elements'));
  liveReference.set('editing', true);
})

// elementDetails.js

app.on('elementDetails', function(evt) {
  evt.original.preventDefault();
  app.set('inspectedElement', app.get(evt.keypath));
  evt.node.blur();
})

// export.js

app.data.printPreviewTemplate = "\n<html>\n\t<head>\n\t\t<style>img {max-width: 100%;}</style>\n\t\t<title>{{ documentTitle }}</title>\n\t\t<link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>\n\t\t<style type=\"text/css\">.print-only{ display: block; } \n\n\n\t\t  .no-section {\n\t\t    display: none;\n\t\t  }\n\n\t\t  .page-sections.no-section {\n\t\t    display: block;\n\t\t  }\n\n\t\t  .blank.reference {\n\t\t\t  display: none;\n\t\t\t}\n\n\t\t\t@page {\n\t\t\t  margin: 1cm;\n\t\t\t}\n\n\t\t\tbody {\n\t\t\t  font-family: 'Source Sans Pro', Helvetica, Arial, sans-serif;\n\t\t\t}\n\n\t\t\t.page-sections, #the-document h1, #the-document h2, #the-document h3, #the-document h4, #the-document h5 {\n\t\t\t  font-family: 'PT Serif', Georgia, serif;\n\t\t\t}\n\t\t\t.navbar-brand, .page .inspector, .inspectors {\n\t\t\t  font-family: 'Source Sans Pro', Helvetica, Arial, sans-serif;\n\t\t\t}\n\n\t\t\t#the-document {counter-reset: level1;}\n\t\t\t#toc:before, #toc:after {counter-reset: level1; content: \"\";}\n\t\t\t#toc h3:before{content: \"\"}\n\t\t\t \n\t\t\t#the-document h2, #toc > ul > li {counter-reset: level2;}\n\t\t\t#the-document h3, #toc > ul >  ul > li {counter-reset: level3;}\n\t\t\t#the-document h4, #toc > ul > ul > ul > li {counter-reset: level4;}\n\t\t\t#the-document h5, #toc > ul > ul > ul > ul > li {counter-reset: level5;}\n\t\t\t#the-document h6, #toc > ul > ul > ul > ul > ul > li {}\n\n\t\t\t#the-document h2:before,\n\t\t\t#toc > ul > li a:before {\n\t\t\t    content: counter(level1) \" \";\n\t\t\t    counter-increment: level1;\n\t\t\t}\n\t\t\t#the-document h3:before,\n\t\t\t#toc > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \" \";\n\t\t\t    counter-increment: level2;\n\t\t\t}\n\t\t\t#the-document h4:before,\n\t\t\t#toc > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \" \";\n\t\t\t    counter-increment: level3;\n\t\t\t}\n\t\t\t#the-document h5:before,\n\t\t\t#toc > ul > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \".\" counter(level4) \" \";\n\t\t\t    counter-increment: level4;\n\t\t\t}\n\t\t\t#the-document h6:before,\n\t\t\t#toc > ul > ul > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \".\" counter(level4) \".\" counter(level5) \" \";\n\t\t\t    counter-increment: level5;\n\t\t\t}\n\t\t</style>\n\t</head>\n\t<body>\n\t\t<div id=\"the-document\">\n\t\t\t{{ content }}\n\t\t</div>\n\t</body>\n</html>";



_.templateSettings = {
  interpolate: /\{\{(.+?)\}\}/g
};

var snapshotHTML = function()
{
  var everything = $('#the-document').clone();
  everything.find('.comments, .section-actions, .toolbar, .no-print').remove();
  var innerHtml = everything.html();
  var fullPageTemplate = _.template(app.data.printPreviewTemplate);
  return fullPageTemplate({ content : innerHtml, documentTitle : app.data.viewTree.name });
}

app.on('print', function() {
  print();
})

app.on('printPreview', function() 
{
  var w = window.open('about:blank', 'printPreview');
  var newDoc = w.document.open("text/html", "replace");
  console.log("writing print preview to new window");
  newDoc.write(snapshotHTML());
  newDoc.close();
  console.log("closed new html stream");
})

app.on('snapshot', function(e, id) 
{
  app.fire('saveSnapshot', id, snapshotHTML());
})

// inspectors.js

app.set('currentInspector', 'document-info');

app.observe('currentInspector', function(newVal) {
  // console.log("inspector change", newVal);
  $('.toggled-inspectors .inspector').hide();
  // console.log("showing #"+newVal);
  $('#'+newVal).show();
})

app.on('showReferencedElement', function(evt) {
  evt.original.preventDefault();
  // TODO actually load in relevant data instead of the hardcoded placeholder
  app.set('currentInspector', 'references');
})

// messages.js

app.on('message', function(type, message) {
  if (console && console.log) {
    console.log('-- ', type, ': ', message);
  }
  if (type === 'error') {
    $('#top-alert').show().find('.message').html(message);
  }
});

app.on('hideIEMessage', function() {
  $('#ie-alert').hide(); 
})

app.on('hideErrorMessage', function() {
  $('#top-alert').hide();
})

// realData.js

// TODO need more data:
// root of the view hierarchy
// comments
//
// where do static assets go? (css, js)
// ^ eventually deployed in some static assets dir
// show placeholder text for blank references (documentation, usually)
// how should we handle url patterns?
// rdfa transclusion?

var viewTree = {

}

app.generatePreviewDocumentation = function(doc)
{
  //console.log("ASDFASD");
  var d = doc.trim();
  
  var preview = "";
  if(d.length > 0) {
        //console.log(String(_.escape(e.documentation)));
        //console.log($(String(_.escape(e.documentation)))); 
        //e.documentationPreview =  "ASD";// $(e.documentation).text();
        //e.documentationPreview = _.escape(e.documentation);
        preview = "<div>" + d + "</div>";
        //console.log($);
        //var asdfasfd = $(e.documentationPreview);
        preview = $(preview).text();//.substring(0, 50) + "...";
        //e.documentationPreview = _.unescape(e.documentationPreview);
        var dots = (preview.length > 200) ? "..." : "";
        preview = preview.substring(0,200) + dots;
        return preview;
    }
    
    return false;
}

app.formatDate = function(d)
{
  return moment(d).format('D MMM YYYY, h:mm a');
}

var parseDate = function(dateString)
{  
  return moment(dateString);
}

app.editableElements = function(section) {
  var elements = {};
  //console.log("VT", viewTree);
  $('.editable[data-property]', section).each(function(i,el)
  {
      var $el = $(el);
      // ignore blanks
      if ($el.hasClass('blank')) {
        return;
      }
      var mdid = $el.attr('data-mdid');
      var data = elements[mdid] || { mdid : mdid };
      var prop = $el.attr('data-property').toLowerCase()
      data[prop] = el.innerHTML;
      // result.push(data);
      elements[mdid] = data;

  });
  return elements;
}

app.generateUpdates = function(section)
{
  var elements = app.editableElements(section);
  _.each(elements, function(e) {
    if(e.hasOwnProperty("documentation")) {
      e.documentation = app.spanContentToBracket(e.documentation);
    }
    e.id = e.mdid;
    delete e["mdid"];
    var sourceEl =app.get("viewTree").elementsById[e.id];
    if(sourceEl.hasOwnProperty("valueType")) {
      e.valueType = sourceEl.valueType;
    }
  });

  //console.log("VT2", viewTree);
  // console.log("elements by id", elements);
  return {"elements": _.values(elements)};
}


app.on('snapshotSuccess', function(snapshotResponse) {
  if(snapshotResponse.constructor.name === 'String')
  {
    snapshotResponse = JSON.parse(snapshotResponse);
  }
  snapshotResponse.formattedDate = app.formatDate(snapshotResponse.created);
  viewTree.snapshots.push(snapshotResponse);
})

var writeBackCache = function()
{
   var elementsToWriteback = [];
   var viewData = {};
   app.observe('postview', function(vd) {
     viewData = vd;
   })

   app.on('saveSection', function(e, sectionId) {   
     var section = $("[data-section-id='" + sectionId + "']");
     //console.log("saveSection", section);
     //var section = document.getElementById(sectionId);
     var updates = app.generateUpdates(section);
     app.fire('saveView', sectionId, updates);
     //console.log("survived");
   })
}();

var buildList = function(object, elements, html) {
  if (!html) html = "<div contenteditable='false'>";
  var listTag = object.ordered ? 'ol' : 'ul';
  html += '<'+listTag+'>';
  // items is a 2D array. first depth is lists, second is multiple values per item
  _.each(object.list, function(itemContents) {
    var listItemContent = "";
    // content can be made of multiple references
    _.each(itemContents, function(item, i) {
      if (item.type != 'List') {
        var val = resolveValue(item, elements);
        listItemContent += '<div class="list-item">'+renderEmbeddedValue(val, elements) + "</div>";

        // push out the list content if it's the last item
        // or if the next item is a list
        if (listItemContent != "" && (i === itemContents.length-1 || itemContents[i+1].type === 'List')) {
          html += "<li>"+listItemContent+"</li>";
          listItemContent = "";
        }
      } else {
        html += buildList(item, elements);
      }
    })

  })
  html += '</'+listTag+'></div>';
  return html;
}

var resolveValue = function(object, elements, listProcessor) {
  
  if (Array.isArray(object)) {
    var valuesArray = _.map(object, function(obj) { return resolveValue(obj, elements) });
    return listProcessor ? listProcessor(valuesArray) : _.pluck(valuesArray, 'content').join("  ");
  } else if (object.sourceType === 'text') {
    return { content : object.text, editable : false };
  // } else if (object.type === 'List') {
  //   return { content : '!! sublist !! ', editable : false };
  } else if(object.sourceType === 'reference') {
    // console.log("resolving ", object.sourceProperty, " for ", object.source, object);

    var source = elements[object.source];
    if (!source) {
      return { content : 'reference missing', mdid : object.source };
    } else if (object.sourceProperty) {

      var referencedValue = source[object.sourceProperty.toLowerCase()];
    } else{
      console.warn("!! no sourceProperty for", object);
    }
    // console.log(referencedValue);
    return { content : referencedValue, editable : true, mdid :  source.mdid, property: object.sourceProperty };
  } else {
    return {content: "Unknown source type", mdid: object.source};
  }
}

var classAttr = function(classes) {
  return 'class="' + classes.join(" ") + '"';
}

var renderEmbeddedValue = function(value, elements) {
  var h = "";
  var ref = elements[value.mdid];
  var title = ref ? (ref.name || ref.mdid) +' ('+value.property.toLowerCase()+')' : '';
  var classes = ['reference'];
  var blankContent = false;
  if(!value.content){
    blankContent = !value.content || value.content === "" || value.content.match(/^\s+$/);
  } 
  if (blankContent) {
    classes.push('blank')
  }
  if (value.editable) {
    classes.push('editable');
    if (value.property == 'documentation') {
      classes.push('doc');
    }
    // TODO use something other than id here, since the same reference can appear multiple times
    //contenteditable="true"
    h += '<div ' + classAttr(classes) + ' data-property="' + value.property + '" data-mdid="' + value.mdid +'" title="'+title+'">';
  } else {
    if (ref) {
      classes.push('not-editable');
      h += '<div ' + classAttr(classes) + ' contenteditable="false" title="'+title+'">';          
    } else if (value.mdid && !ref) {
      h += '<div class="missing" contenteditable="false">';
    } else {
      h += '<div class="literal" contenteditable="false">';
    }
  }
  h += blankContent ? 'no content for ' + (ref.name || ref.id) + ' ' + value.property.toLowerCase() : value.content;
  //h += blankContent ? '' : value.content;
  h += '</div>';
  return h;
}


var addChildren = function(parentNode, childIds, view2view, views, elements, depth) {
  // console.log("iterating through", childIds);
  // return;
  for (var idx in childIds) {
    var id = isNaN(idx) ? idx : childIds[idx];
    // console.log("handing", id);
    var child = { id : id, children: [], viewData : app.get('viewsById')[id] };
    child.name = app.get('elementsById')[id].name;
    // resolve referenced content
    child.content = "";
    child.depth = depth;
    child.class = child.viewData.noSection ? 'no-section' : '';
    child.viewData.modifiedFormatted = app.formatDate(parseDate(child.viewData.lastModified));

    // console.log("contains:", child.viewData.contains);
    for (var cIdx in child.viewData.contains) {     
      var c = child.viewData.contains[cIdx];
      if (c.type == 'Table') {
        // console.log("skipping table...");
        var table = '<div contenteditable="false"><table class="table table-striped">';
        table += '<caption>'+c.title+'</caption>';
        table += "<thead>";
        for(var rIdx in c.header) {
          table += "<tr>";
          for (var hIdx in c.header[rIdx]) {
            var cell = c.header[rIdx][hIdx];
            var value = resolveValue(cell.content, elements, function(valueList) {
              return _.map(valueList, function(v) { return renderEmbeddedValue(v, elements) }).join("");
            });
            // console.log("header value", value)
            table += '<th colspan="'+ (cell.colspan || 1) + '" rowspan="' + (cell.rowspan || 1) + '">' + value + "</th>";
          }
          table += "</tr>";
        }
        table += "</thead>"
        table += "<tbody>";
        for (var rIdx in c.body) {
          table += "<tr>";
          for (var cIdx in c.body[rIdx]) {
            var cell = c.body[rIdx][cIdx];
            var value = resolveValue(cell.content, elements, function(valueList) {
              var listOfElements = _.map(valueList, function(v) { return renderEmbeddedValue(v, elements) });
              var stringResult = ""; //<ul class='table-list'>";
              _.each(listOfElements, function(e){
                stringResult += e + "<br/>"; //"<li>" + e + "</li>";
              })
              //stringResult += "</ul>";
              return stringResult;
            });
            // TODO need to pull out the renderer here so that we can do multiple divs in a cell
            table += '<td colspan="'+ (cell.colspan || 1) + '" rowspan="' + (cell.rowspan || 1) + '">' + value + "</td>";
          }
          table += "</tr>";
        }
        table += "</tbody>"
        table += "</table></div>"
        child.content += table;
      } else if (c.type === 'List') {
        child.content += buildList(c, elements);        
      } else {
        
        var value = resolveValue(c, elements);
        child.content += renderEmbeddedValue(value, elements);
        
      }
      
    }
    
    if (view2view[id]) {
      addChildren(child, view2view[id], view2view, views, elements, depth+1)
    }
    parentNode.children.push(child);
    // console.log("added", child, " to parent ", parentNode);
  }
}


var constructOrderedChildren = function(node)
{
  var result = [node];
  _.each(node.children, function(c) {
    //result.push(c);
    //console.log("alex", result, constructOrderedChildren(c))
    result = result.concat(constructOrderedChildren(c));
  });
  return result;
}

app.observe('home', function(homeData)
 {
  var homeTree = { name: homeData.name, children: [] };
  _.each(homeData.projectVolumes, function(pv)
  {
    var child = {};
    child.id = pv;
    child.name = homeData.volumes[pv];
    child.children = [];
    homeTree.children.push(child);
  }) 

  var buildHomeTree = function(nodeList)
  {
    _.each(nodeList, function(node)
    {
      var childIds = [];
      if(node.id in homeData.volume2volumes)
      {
        childIds = childIds.concat(homeData.volume2volumes[node.id])
      }
      if(node.id in homeData.volume2documents)
      {
        childIds = childIds.concat(homeData.volume2documents[node.id])
      }
      _.each(childIds, function(cid)
      {
        var child = {};
        child.id = cid;
        child.children = [];
        if(cid in homeData.volumes)
        {
          child.name = homeData.volumes[cid];
        }
        if(cid in homeData.documents)
        {
          child.name = homeData.documents[cid];
        }
        node.children.push(child);
      })
      node.showLink = node.children.length == 0;
      node.hidden = node.name === 'Unexported Document';
      buildHomeTree(node.children)
    })
  }
  buildHomeTree(homeTree.children)
  // console.log("final home tree", homeTree)
  app.set('homeTree', homeTree)
 })

app.observe('viewHierarchy', function(viewData) {

  // Hack, add an mdid proprety to each view that is equal to it's id
  _.each(viewData.views, function(curv) {
    curv.mdid = curv.id;
  })

  // Hack, add an mdid proprety to each element  that is equal to it's id
  _.each(viewData.elements, function(cure) {
    cure.mdid = cure.id;
    if(cure.hasOwnProperty("value")) {
      cure.value = cure.value[0];
    }

  })

    // Fill in values for all transcluded span tags in documentation fields
  _.each(viewData.elements, function(cure) {
    if(cure.hasOwnProperty('documentation')) {
      cure.documentation = app.spanContentToValue(cure.documentation, viewData);
    }
  })
  
  // index views by id
  var viewsById = {};
  for (var idx in viewData.views) {
    var view = viewData.views[idx];
    view.noSection = false;
    viewsById[view.mdid] = view;
  }
  _.each(viewData.noSections, function(id) {
    viewsById[id].noSection = true;
  });


  app.set('viewsById', viewsById);
  // index elements by id
  var elementsById = {};
  for (var idx in viewData.elements) {
    var e = viewData.elements[idx];
    elementsById[e.mdid] = e;
  }
  app.set('elementsById', elementsById);

  // format snapshot dates
  if (viewData.snapshots) {
    _.each(viewData.snapshots, function(snapshot) {
      snapshot.formattedDate = app.formatDate(snapshot.created);
    })
  }

  // For now, just translate the new View2View api so that it looks like the old one
  var viewMapping = {};
  _.each(viewData.view2view, function(curv) {
    viewMapping[curv.id] = curv.childrenViews;
  })
  viewData.view2view = viewMapping;
  viewData.rootView = viewData.id;

  // // app.set('debug', JSON.stringify(viewData));
  // console.log(viewData);
  // TODO: Change addChildren to construct the parentNode content instead of the childrenNodes
  // Then we could just pass viewTree instead of tempTree
  var tempTree = {"children" : []};
  addChildren(tempTree, [viewData.rootView], viewData.view2view, viewData.views, elementsById, 0);
  // console.log("tempTree", tempTree);

  viewTree = tempTree.children.length > 0 ? tempTree.children[0] : [];
  viewTree.orderedChildren = constructOrderedChildren(viewTree);
  viewTree.snapshot = viewData.snapshot;
  if(viewTree.snapshot === true)
  {
    viewTree.snapshoted = app.formatDate(parseDate(viewData.lastModified));
  }
  viewTree.snapshots = viewData.snapshots;
  viewTree.elements = viewData.elements;

  viewTree.elements = _.sortBy(viewTree.elements, function(e){ return e.qualifiedName });


  viewTree.elementsById = elementsById;
  // Create a list of transcludable elements
  _.each(viewTree.elements, function(e) {

    // set string for client side searching
    e.searchIndex = e.qualifiedName.toLowerCase();

    // Remove name after last slash for   
    if(e.hasOwnProperty('qualifiedName')) {
      e.qualifiedNamePreview = e.qualifiedName.substr(0, e.qualifiedName.lastIndexOf("/"));
      e.qualifiedNameParentPreview =  e.qualifiedNamePreview.substr(e.qualifiedNamePreview.lastIndexOf("/")+1, e.qualifiedNamePreview.length);
    
    }

    //console.log(e);
    if(e.hasOwnProperty('documentation')) {
      var preview = app.generatePreviewDocumentation(e.documentation);
      //console.log(preview);
      if(preview){
        e.documentationPreview = preview;
      }
    }
  });

  app.set('viewTree', viewTree, function() {
    setTimeout(function() { 
      app.fire('makeToc');
    }, 0);
  });
})



// rich-reference.js

var _keys = function(obj) {
  var keys = [];
  for(var k in obj) keys.push(k);
  return keys;
}

// rich reference editor
app.createLiveReference = function($el, elementRef, elementList) {
  if ($el.length == 0) return;
  // if we don't have table data, try and extract it from the element
  // console.log("tablefying", $el);
  // if (!tableData) tableData = JSON.parse($el.find('[data-table-data]').attr('data-table-data'));

  var liveReference = new Ractive({
    el: $el[0],
    template: app.get('referenceTemplate'),
    data: { 
      elementList : elementList,
      elementRef : undefined,
      displayedValue : undefined,
      serialize : function(id, attribute) {
        return JSON.stringify({ elementId : id, elementAttribute : attribute}).replace(/"/g,'\"');
      }
    }

  })

  console.log("elements to choose from:", liveReference.get('elementList'));

  liveReference.observe('elementRef', function(ref) {
    console.log("elementRef:", ref)
    console.log("elementList",liveReference.get('elementList'))
    var element = liveReference.get('elementList.'+ref.id);
    console.log("element",element, "keys", _keys(element));
    if (element) {
      var keys = _keys(element);
      liveReference.set('attributeOptions', keys);
      if (!liveReference.get('elementRef.attribute')) {
        ref.attribute = keys[0];
      }
    }
    liveReference.set('displayedValue', liveReference.get('elementList.'+ref.id+'.'+ref.attribute));
    console.log("displayedValue", liveReference.get('displayedValue'));
  })

  liveReference.on('editRichElement', function(e) {
    console.log("editing rich element!", e);
    liveReference.set('editing', true);
  })

  liveReference.on('save', function() {
    console.log('saving reference');
    liveReference.set('editing', false);
  })

  liveReference.on('cancel', function() {
    // TODO actually revert the content, or remove if it was just created
    liveReference.set('editing', false);
  })

  liveReference.on('delete', function() {
    $(liveReference.el).detach();
  })

  return liveReference;

}


// rich-table.js

// rich table editor
app.createLiveTable = function($el, tableData) {
  if ($el.length == 0) return;
  // if we don't have table data, try and extract it from the element
  console.log("tablefying", $el);
  if (!tableData) tableData = JSON.parse($el.find('[data-table-data]').attr('data-table-data'));

  var toCSV = function(rowArray) {
    if (!rowArray) return "";
    var text = "";
    var d = ',';
    for (var i=0; i<rowArray.length; i++) {
      // text += rowArray[i].join(d)+"\n";
      var row = rowArray[i];
      var cells = []
      for (var j=0; j<row.length; j++) {
        cells.push(row[j].replace(/,/g, '\,'));
      }
      text += cells.join(d);
      if (i != rowArray.length) text += '\n'
    }
    return text;
  }

  var fromCSV = function(text) {
    // this doesn't handle double commas properly
    var d = /([^\\]),/g;
    var rows = [];
    var lines = text.split("\n");
    for (var i=0; i<lines.length; i++) {
      // var cells = lines[i].split(d);
      lines[i] = lines[i].replace(d, '$1|');
      console.log(lines[i]);
      var cells = lines[i].split("|");
      var row = [];
      for (var j=0; j<cells.length; j++) {
        row.push(cells[j].replace(/\\,/g,','));
      }
      rows.push(row);
    }
    return rows;
  }

  var liveTable = new Ractive({
    el: $el[0],
    template: app.get('tableTemplate'),
    data: { 
      tableData : tableData,
      source : toCSV(tableData),
      serialize : function(rowArray) {
        return JSON.stringify(rowArray).replace(/"/g,'\"');
      }
    }

  })

  liveTable.observe('source', function(source) {
    // quick hack for tab-delimited data
    if (source.indexOf('\t') != -1) {
      source = source.replace(/,/g,'\\,');
      source = source.replace(/\t/g,',');
      console.log("source is now:", source)
      liveTable.set('source', source);
    }

    var rows = fromCSV(source);
    liveTable.set('tableData', rows);
    liveTable.set('headerRow', rows[0]);
    liveTable.set('bodyRows', rows.slice(1));
  })

  liveTable.on('editRichElement', function(e) {
    console.log("editing rich element!", e);
    liveTable.set('editing', true);
  })

  liveTable.on('save', function() {
    console.log('saving table');
    liveTable.set('editing', false);
  })

  liveTable.on('cancel', function() {
    // TODO actually revert the content, or remove if it was just created
    liveTable.set('editing', false);
  })

  liveTable.on('delete', function() {
    $(liveTable.el).detach();
  })

  return liveTable;

}


// search.js


var search = _.debounce(
  function(q){
  var searchStyle = document.getElementById('search_style');
  if (!q) {
    searchStyle.innerHTML = "";
    return;
  }

  // tokenize the search (to lower case so we're case insensitive matching how things are placed into index)
  q = q.toLowerCase().replace('"',"");
  var split = q.split(" ");
  var selector = "";

  // add a new css selector for each token to hide anyting that doesn't have that token
  for(var i in split)
  {
    // don't do anything if there's no token
    if(split[i].trim() == "")
    {
      continue;
    }
    selector += '.searchable:not([data-search-index*="' +split[i]+ '"]){ display : none; }';
  }
  searchStyle.innerHTML = selector; //".searchable:not([data-search-index*=\"" + q + "\"]) { display: none; }";
}, 250);

app.observe('transcludableQuery', function(q) {
  search(q);
})

// sections.js

app.observe('plan_sections', function(newText) {
  // console.log("new text", newText);
  var sections = [];
  var $doc = $('<div class="doc"></div>').html(newText);
  $doc.find('section').each(function(i, section) {
    // console.log(i, section);
    sections.push({ id : "section-"+i, content : section.innerHTML });
  });
  // console.log("sections", sections)
  app.set('plan_sections_list', sections);
})

// svg.js


// Note:  This code was cleaner but FF has a bug where getBBox only works on visible elements
// so svg-edit cannot be initialized while the modal his hidden
// Instead we have a complicated event chain to lazily initialize svg-edit on first use


// Callback to signify svg edit dom elements have completed initialization
window.editorOnLoad = function(arg)
{
  app.fire("svgEditInit");
}

// Create an object to manage the svg-editor
window.svgEditManager = function()
{
  // State variables
  var curDocSvg;
  var curDeleteOnCancel;  // used to delete a new svg element if it's being created and user selects cancel
  var svgCanvas = null;
  var iframe = null;

  // Setup svg editor html in modal
  var editorContainer = null;
  app.on("svgEditCreate", function() {
    // If this is the first time, create the editor.  onload="editorOnLoad" will trigger a callback to run svgEditInit to finish our initialization.
    // svgEditInit will trigger svgEditUpdateContent to update the content of the editor once fully initialized 
    if(editorContainer === null) {
      editorContainer = $(".modal-body").html("<div class='editor-container'></div>").find(".editor-container");
      editorContainer.append('<div class="mini-toolbar"><button class="btn btn-default btn-sm" id="svgDelete">delete</button><div class="pull-right btn-group"><button class="btn btn-default btn-sm" id="svgCancel" type="button">Cancel</button><button class="btn btn-default btn-sm" type="button" id="svgSave">Save</button></div></div>');
      editorContainer.append('<iframe id="svg-editor-iframe" src="${url.context}/scripts/vieweditor/vendor/svgedit/svg-editor.html" width="100%" onload="editorOnLoad(this)" height="600px"></iframe>');

      // Helper
      var cleanupAndExit = function() {
        svgCanvas.clear();
        $('#myModal').modal('hide');
      }

      // Setup save delete and cancel buttons
      editorContainer.find("#svgCancel").click(function() {
        if(curDeleteOnCancel === true)
        {
          curDocSvg.remove();
        }
        cleanupAndExit();
      });

      editorContainer.find("#svgDelete").click(function() {
        curDocSvg.remove();
        cleanupAndExit();
      });

      editorContainer.find("#svgSave").click(function() {
        var canvas = editorContainer.find("iframe").data('canvas');
        canvas.getSvgString()(function(data, error) {
            if (error) {
              app.fire('message', 'error', error);
            } else {
              var parent = curDocSvg.parent();    // get the parent before we replace
              curDocSvg.replaceWith(data); 
              curDocSvg = null; // null out reference since we replaced it
              addSvgClickHandler(parent.find('svg'));
              cleanupAndExit();   
            }
          });
      });
    } 
    else {
      // If the editor is already setup manually trigger content update
      app.fire("svgEditUpdateContent");
    }
  });

  // Init svg canvas and update content
  app.on("svgEditInit", function() {
    iframe = editorContainer.find("#svg-editor-iframe");
    svgCanvas = new embedded_svg_edit(iframe.get(0));
    iframe.data('canvas', svgCanvas);
    // Hide main button, as we will be controlling new/load/save etc from the host document
    var doc;
    doc = iframe.get(0).contentDocument;
    if (!doc)
    {
      doc = iframe.get(0).contentWindow.document;
    }      
    var mainButton = doc.getElementById('main_button');
    mainButton.style.display = 'none';
    app.fire("svgEditUpdateContent");
  });

  // Update the content of the editor
  app.on("svgEditUpdateContent", function() {
      var serializer = new XMLSerializer();
      var svgString = serializer.serializeToString(curDocSvg.get(0));
      svgCanvas.setSvgString(svgString);
  });

  return {
    edit : function(docSvg, deleteOnCancel)
    {
      // Register handler to ensure we don't continue on event chain until modal is shown.  FF bug prevents us from doing init while hidden
      $('#myModal').on('shown.bs.modal', function () {
        $('#myModal').on('shown.bs.modal', function () {}); // unregister handler
        app.fire("svgEditCreate");      // Start event chain to lazily init svg-edit and update content
      })
      curDocSvg = docSvg;
      curDeleteOnCancel = deleteOnCancel;
      $('#myModal').modal('show'); // Note this has to happen in FF before svg-edit can be initialized, just to make things exciting
    }
  }
  
}();

// This event gets called on the section element when the edit button is pressed
// It will add a click handler to all svg elements to trigger the modal
app.on('initializeSvgEditor', function(el) {
  addSvgClickHandler($(el).find('svg'));

  // TODO strip contenteditable attribute before saving
})

// Called when svg button is pressed to insert new svg and open editor modal
window.insertSvg = function(sectionId) {
  var dummyButton = $('[data-role=editor-toolbar][data-target="#section' + sectionId + '"]').find(".dummyButton");
  dummyButton.click();

  document.execCommand('insertHTML', false,  '<svg class="new-svg" contenteditable="false" style="display: inline" width="640" height="480" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg"><g></g></svg>');
  var svg = $('svg.new-svg');
  svg.removeAttr("class");    // jquery svg bug prevents simply svg.removeClass('new-svg');
  svgEditManager.edit(svg, true);
} 

// Add a click handler to existing svg elements to trigger modal
window.addSvgClickHandler = function(el) {
  el.attr('contenteditable', false).click(function() {
    svgEditManager.edit($(this), false);
  });
}


// toc.js

app.on('makeToc', function() {
  $("#toc").tocify({ selectors: "h2.numbered-header, h3.numbered-header, h4.numbered-header, h5.numbered-header", history : false, scrollTo: "60", hideEffect: "none", showEffect: "none", highlightOnScroll: true, highlightOffset : 0, context: "#the-document", smoothScroll:false, extendPage:false }).data("toc-tocify");  
})

// Hack needed to fix inspector width due to bug in bootstrap affix when inspector is on the left side of the page
app.resizeInspector = function() {
    var $affixElement = $(".affix");
    $affixElement.width($affixElement.parent().width());
}
$(window).resize(app.resizeInspector);
app.resizeInspector();

// note sure why but transclude.js doesn't load properly unless we have a log line here?
console.log("");
/*
(function poll() {
    setTimeout(function() {
        $.ajax({
            url: absoluteUrl('/ui/relogin'),
            type: "GET",
            success: function(data) {
                if (data.indexOf("html") != -1) 
                  alert("You've been logged out! Login in a new window first!");
          window.open("/alfresco/faces/jsp/login.jsp?_alfRedirect=/alfresco/service/ui/relogin");
            },
            complete: poll,
            timeout: 5000
        })
    }, 60000);
})();
*/

// transclusion.js




var lastTransLength = 0;

//app.on("imcool", function(e)  {
//  console.log("Thats right");
  //saveSelection();
//})

app.on("viewTree", function(vt) {
  console.log("Load", vt);
})
//transclusionClick
app.on('transclusionDown', function(e) {
  //restoreSelection();
  //  document.execCommand("insertHTML", false, "IMAREFERENCE!!!");

  //$(".transcludable").on('click', function() {
    //console.log("-----");
    //saveSelection();
    //console.log("ASDF!!!", e);
    
    var savedSel = app.getSavedSelection();
    //console.log("TransDown", savedSel);
    if(savedSel.length > 0){
      var section = $(savedSel[0].commonAncestorContainer).closest(".section.page.editing");
      //var section = $(app.getSelectedNode()).closest(".section.page.editing");
      var sectionId = section.attr("data-section-id");
      //console.log(sectionId);
      var tran = $(e.node);
      //console.log("tran", tran);
      var transId = tran.attr("data-trans-id");
      //console.log("tid", transId);
      
      var refType = "";
      if(tran.hasClass("name")){
        refType = "name";
      }else if(tran.hasClass("documentation")){
        refType = "documentation";
      } else if(tran.hasClass("dvalue")){
        refType = "value";
      }

      //var dummyButton = $('[data-role=editor-toolbar][data-target="#section' + sectionId + '"]').find(".dummyButton");
      //dummyButton.click();
      
      //var vtree = app.get("viewTree");
      //var elem = _.filter(vtree.elements, function(curElem) {
      // return curElem.mdid === transId;
      //})
      //elem = elem[0];
      var name = $('[data-trans-id="'+transId+'"].transcludable.name').text();
      //console.log(name);
      var r = '["'+name+'":'+refType+':'+transId+'] ';
      app.restoreSelection();
      document.execCommand("insertHTML", false, r);
      lastTransLength = r.length;


    }  
    //console.log("TransDown done");
    
    //console.log("-----");
    //saveSelection();
    //if(savedSel.length > 0){
  //    var section = $(savedSel[0].commonAncestorContainer).closest(".section.page.editing");//$(savedSel.commonAncestorContainer).closest(".section.page.editing");
  //    console.log(section.attr("data-section-id"));
    //  restoreSelection();
    //  document.execCommand("insertHTML", false, "IMAREFERENCE!!!");
    //}
    //*/
  //})
})

app.on('transclusionUp', function(e) {
  //console.log("Start Up");
  app.restoreSelection();

  //setTimeout(function() {
      //var advanceLength = r.length;
  if(lastTransLength !== 0) {

    var range = window.getSelection().getRangeAt(0);
    //console.log(range.startOffset, range.startContainer);
    //console.log("target : " + (range.startOffset + lastTransLength) + " : ", range.startContainer );
    range.setStart(range.startContainer, range.startOffset + lastTransLength);
    //console.log("Good1");
    range.setEnd(range.startContainer, range.startOffset + 0);

    window.getSelection().removeAllRanges();
    window.getSelection().addRange(range);
  }
  lastTransLength = 0;
 // },100);
  app.saveSelection();

  //console.log("End Up");
})

//console.log("ASDFASFD");
</script>
</body>
</html>