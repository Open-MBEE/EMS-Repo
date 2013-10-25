<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>View Editor</title>
    <link rel="stylesheet" href="${url.context}/scripts/vieweditor/vendor/css/bootstrap.min.css" media="screen">
    <link href="${url.context}/scripts/vieweditor/styles/jquery.tocify.css" rel="stylesheet" media="screen">
    <link href="${url.context}/scripts/vieweditor/styles/styles.css" rel="stylesheet" media="screen">
    <link href="${url.context}/scripts/vieweditor/styles/print.css" rel="stylesheet" media="print">
    <link href="${url.context}/scripts/vieweditor/styles/fonts.css" rel="stylesheet">
    <link href="${url.context}/scripts/vieweditor/vendor/css/whhg.css" rel="stylesheet" >
    <link href="${url.context}/scripts/vieweditor/styles/section-numbering.css" rel="stylesheet">
    <link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>
  
<script type="text/javascript">
var pageData = { viewHierarchy: ${res},  baseUrl: "${url.context}/wcs" };
</script>

</head>

  <body class="{{ meta.pageName }} {{ settings.currentWorkspace }}">
<div id="main"></div>
<script id="template" type="text/mustache">

    <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="navbar-header">
          {{#environment.development}}
            <a class="navbar-brand" href="/">Europa View Editor {{ title }}</a>
          {{/environment.development}}
          {{^environment.development}}
            <a class="navbar-brand" href="${url.context}/wcs/ui/">Europa View Editor {{ title }}</a>
          {{/environment.development}}
      </div>
      <ul class="nav navbar-nav">
        {{#environment.development}}
        <li><a href="dashboard.html">dashboard</a></li>
        <li><a href="about.html">about</a></li>
        {{/environment.development}}
        {{^environment.development}}
        <li><a href="${url.context}/wcs/ui/">dashboard</a></li>
        {{/environment.development}}
      </ul>


      <div class="pull-right">
        <a href="vision.html"><img class="europa-icon" src="${url.context}/scripts/vieweditor/images/europa-icon.png" /></a>
      </div>

      <!-- 
      <form class="navbar-form navbar-right" action="">
        <div class="form-group">
          <select id="workspace-selector" class="form-control input-sm" value="{{ settings.currentWorkspace }}">
            <option value="modeler">Modeler</option>
            <option value="reviewer">Reviewer</option>
            <option value="manager">Manager</option>
          </select>
        </div>
      </form>
    -->

    </nav>

    <div id="top-alert" class="alert alert-danger alert-dismissable" style="display:none">
      <button type="button" class="close" proxy-click="hideErrorMessage" aria-hidden="true">&times;</button>
      <span class="message"></span>
    </div>

    <div class="wrapper">
      <div class="row split-view">

<div class="col-xs-8">
  <div id="the-document">

    {{#viewTree.orderedChildren}}


            <a name="{{id}}" style="display: block; position:relative; top:-60px; "></a>
            {{#(depth == 0) }}
              {{{("<h1><span class='"+class+" section-header editable' data-property='NAME' data-section-id='" + id + "'>" +  name + "</span></h1>" )}}}
            {{/(depth == 0) }}
            {{^(depth == 0) }}
              {{{("<h"+ (depth+1) + " class='"+class+"'><span class='section-header' data-section-id='" + id + "'><span class='editable' data-property='NAME' data-mdid='" + id + "'>" +  name + "</span></span></h"+ (depth+1) + ">" )}}}
            {{/(depth == 0) }}
           
            <div class="author {{ class }}">Edited by <span class="author-name" data-mdid="{{id}}">{{ viewData.author }}</span></div>
            <div class="modified {{ class }}" data-mdid="{{id}}">{{( viewData.modifiedFormatted )}}</div>
             <a href="${'#'}{{id}}" title="permalink"><i class='glyphicon glyphicon-link'></i>&nbsp;</a>

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
                    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
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
                <button type="button" class="btn btn-default" title="Insert SVG" onclick="insertSvg();">svg</button>
              </div>
              <a class="btn btn-default dummyButton" style="visibility:hidden" data-edit="insertHTML &nbsp;" title="dumb"></a>


              <div class="btn-group pull-right">
                <button type="button" class="btn btn-default" proxy-click="cancelEditing">Cancel</button>
                <button type="button" class="btn btn-primary" proxy-click="saveSection:{{ id }}">Save changes</button>
              </div>
            </div>
            <div id="section{{ id }}" class="section page editing" data-section-id="{{ id }}" contenteditable="true" proxy-dblclick="sectionDoubleClick">
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
                      <div class="comment-info"><small>{{ author }}, {{ modified }}<small></div>
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
                    <button type="button" class="btn btn-primary" proxy-click="addComment:{{id}}">Add comment</button>
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

  <div class="col-xs-4">
    <div class="toggled-inspectors inspectors affix page col-xs-4 no-print" style="height:80%;">

      <select class="form-control" value="{{ currentInspector }}">
        <option value="document-info">Table of Contents</option>
        <!-- <option value="history">History</option> -->
        <!-- <option value="references">References</option> -->
        <option value="export">Export</option>
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

      <div id="history" class="inspector">
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
      <div id="export" class="inspector">
        <h3>Export</h3>
        <ul class="list-unstyled">
          <li><button type="button" class="btn btn-default" proxy-click="print">Print PDF</button></li>         
          <li><button type="button" class="btn btn-default" proxy-click="printPreview">Print Preview</button></li>
          {{^viewTree.snapshot}}
          <li><button type="button" class="btn btn-default" proxy-click="snapshot:{{(viewTree.id)}}">Snapshot</button></li>   
          {{/viewTree.snapshot}}       
        </ul>

        <ul class="list-unstyled">
        {{#viewTree.snapshots}}
          <li><a href="{{ url }}">{{ formattedDate }} &mdash; {{ creator }}</a></li>
        {{/viewTree.snapshots}}
        </ul>
      </div>

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
<script type="text/javascript" src="${url.context}/scripts/vieweditor/vendor/Ractive.js"></script>
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
    .done(function() { app.fire('message', 'success', successMessage); })
    .fail(function(e) { 
      if (e && e.status && e.status === 200) {
        // we got a 200 back, but json parsing might have failed
        return;
      } else {
        app.fire('message', 'error', errorMessage); 
        if (console && console.log) {
          console.log("ajax error:", e);
        }        
      }
    })
}

app.on('saveView', function(viewId, viewData) {
  var jsonData = JSON.stringify(viewData);
  var url = absoluteUrl('/ui/views/' + viewId);
  ajaxWithHandlers({ 
    type: "POST",
    url: url,
    data: jsonData,
    contentType: "application/json; charset=UTF-8"
  }, "Saved view", "Error saving view");
})

app.on('saveComment', function(evt, viewId, commentBody) {
  var url = absoluteUrl("/ui/views/"+viewId+"/comment");
  ajaxWithHandlers({ 
    type: "POST",
    url: url,
    data: commentBody,
    contentType: "text/plain; charset=UTF-8"
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
    commentField.focus();    
    selectBlank(commentField);
  } else {
    comments.hide();
  }
});

app.on('addComment', function(evt, mbid) {
  var commentFieldId = "#comment-form-" + mbid;
  var commentField = $(commentFieldId);
  commentField.find('.placeholder').detach();
  var newCommentBody = commentField.cleanHtml();
  if (newCommentBody != "") {
    app.get(evt.keypath+".viewData.comments").push({ author : 'You', body : newCommentBody, modified : new Date()});

    app.fire('saveComment', null, mbid, newCommentBody);
  }
  app.placeholder(commentField, "Type your comment here");
});

// editor.js

app.getSelectedNode = function() {
  return document.selection ? document.selection.createRange().parentElement() : window.getSelection().anchorNode.parentNode;
}

app.on('editSection', function(e, sectionId) {

  e.original.preventDefault();
  // TODO turn editing off for all other sections?
  app.set(e.keypath+'.editing', true);
  var section = $("[data-section-id='" + sectionId + "']");

  var sectionHeader = section.filter('.section-header');
  sectionHeader.data('original-content', sectionHeader.html());
  // bind toolbar properly, no toolbar for section name
  section.filter('.section.page').wysiwyg({toolbarSelector: '[data-role=editor-toolbar][data-target="#section' + sectionId + '"]'});
  section.filter('span').wysiwyg({toolbarSelector : '#no-toolbar'});
  
  // TODO turn this listener off on save or cancel
  section.on('keyup paste blur',function(evt) {
    // we need to use the selection api because we're in a contenteditable
    var editedElement = app.getSelectedNode();
    var $el = $(editedElement);
    var mdid = $el.attr('data-mdid');
    var property = $el.attr('data-property');
    var newValue = $(editedElement).html();
    // TODO filter out html for name and dvalue?
    // find others, set their values
    $('[data-mdid='+mdid+'][data-property='+property+']').not($el).html(newValue);
  })

  // handle placeholder text
  // TODO remove this listener on cancel or save
  section.click(function() {
    var $el = $(app.getSelectedNode());
    if ($el.is('.editable.reference.blank')) {
      $el.html('&nbsp;');
      $el.removeClass('blank');
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
  e.original.preventDefault();
  var sectionId = app.get(e.keypath+'.id');
  app.set(e.keypath+'.editing', false);
  // console.log("canceling", sectionId);
  var $sectionHeader = $('.section-header[data-section-id="'+sectionId+'"]');
  // console.log("canceled editing for section header", $sectionHeader);
  $sectionHeader.html($sectionHeader.data('original-content'));
  $sectionHeader.attr('contenteditable', false);
  // app.set(e.keypath+'.content', app.get(e.keypath+'.previousContent'));
  // console.log("canceled", app.get(e.keypath));
})

app.on('saveSection', function(e, sectionId) {
  e.original.preventDefault();

  $('.modified[data-mdid="' + sectionId+ '"]').text(app.formatDate(new Date()));
  $('.author-name[data-mdid="' + sectionId+ '"]').text("You");


  var section = $("[data-section-id='" + sectionId + "']");
  //console.log("savesection", section);
  app.set(e.keypath+'.name', section.filter(".section-header").html());
  app.set(e.keypath+'.content', section.filter(".section").html());
  app.set(e.keypath+'.editing', false);
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

app.data.printPreviewTemplate = "\n<html>\n\t<head>\n\t\t<title>{{ documentTitle }}</title>\n\t\t<link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>\n\t\t<style type=\"text/css\">\n\n\n\t\t  .no-section {\n\t\t    display: none;\n\t\t  }\n\n\t\t  .page-sections.no-section {\n\t\t    display: block;\n\t\t  }\n\n\t\t  .blank.reference {\n\t\t\t  display: none;\n\t\t\t}\n\n\t\t\t@page {\n\t\t\t  margin: 1cm;\n\t\t\t}\n\n\t\t\tbody {\n\t\t\t  font-family: 'Source Sans Pro', Helvetica, Arial, sans-serif;\n\t\t\t}\n\n\t\t\t.page-sections, #the-document h1, #the-document h2, #the-document h3, #the-document h4, #the-document h5 {\n\t\t\t  font-family: 'PT Serif', Georgia, serif;\n\t\t\t}\n\t\t\t.navbar-brand, .page .inspector, .inspectors {\n\t\t\t  font-family: 'Source Sans Pro', Helvetica, Arial, sans-serif;\n\t\t\t}\n\n\t\t\t#the-document {counter-reset: level1;}\n\t\t\t#toc:before, #toc:after {counter-reset: level1; content: \"\";}\n\t\t\t#toc h3:before{content: \"\"}\n\t\t\t \n\t\t\t#the-document h2, #toc > ul > li {counter-reset: level2;}\n\t\t\t#the-document h3, #toc > ul >  ul > li {counter-reset: level3;}\n\t\t\t#the-document h4, #toc > ul > ul > ul > li {counter-reset: level4;}\n\t\t\t#the-document h5, #toc > ul > ul > ul > ul > li {counter-reset: level5;}\n\t\t\t#the-document h6, #toc > ul > ul > ul > ul > ul > li {}\n\n\t\t\t#the-document h2:before,\n\t\t\t#toc > ul > li a:before {\n\t\t\t    content: counter(level1) \" \";\n\t\t\t    counter-increment: level1;\n\t\t\t}\n\t\t\t#the-document h3:before,\n\t\t\t#toc > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \" \";\n\t\t\t    counter-increment: level2;\n\t\t\t}\n\t\t\t#the-document h4:before,\n\t\t\t#toc > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \" \";\n\t\t\t    counter-increment: level3;\n\t\t\t}\n\t\t\t#the-document h5:before,\n\t\t\t#toc > ul > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \".\" counter(level4) \" \";\n\t\t\t    counter-increment: level4;\n\t\t\t}\n\t\t\t#the-document h6:before,\n\t\t\t#toc > ul > ul > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \".\" counter(level4) \".\" counter(level5) \" \";\n\t\t\t    counter-increment: level5;\n\t\t\t}\n\t\t</style>\n\t</head>\n\t<body>\n\t\t<div id=\"the-document\">\n\t\t\t{{ content }}\n\t\t</div>\n\t</body>\n</html>";

_.templateSettings = {
  interpolate: /\{\{(.+?)\}\}/g
};

var snapshotHTML = function()
{
  var everything = $('#the-document').clone();
  everything.find('.comments, .section-actions, .toolbar').remove();
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

app.formatDate = function(d)
{
  return moment(d).format('D MMM YYYY, h:mm a');
}

var parseDate = function(dateString)
{  
  return moment(dateString);
}

app.generateUpdates = function(section)
{
  var elements = {};
  $('.editable[data-property]', section).each(function(i,el)
  {
      var $el = $(el);
      // ignore blanks
      if ($el.hasClass('blank')) {
        return;
      }
      var mdid = $el.attr('data-mdid');
      var data = elements[mdid] || { mdid : mdid };
      data[$el.attr('data-property').toLowerCase()] = el.innerHTML;
      // result.push(data);
      elements[mdid] = data;
  });
  // console.log("elements by id", elements);
  return _.values(elements);
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
  if (!html) html = "";
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
  html += '</'+listTag+'>';
  return html;
}

var resolveValue = function(object, elements, listProcessor) {
  if (Array.isArray(object)) {
    var valuesArray = _.map(object, function(obj) { return resolveValue(obj, elements) });
    return listProcessor ? listProcessor(valuesArray) : _.pluck(valuesArray, 'content').join("  ");
  } else if (object.source === 'text') {
    return { content : object.text, editable : false };
  // } else if (object.type === 'List') {
  //   return { content : '!! sublist !! ', editable : false };
  } else {
    // console.log("resolving ", object.useProperty, " for ", object.source, object);
    var source = elements[object.source];
    if (!source) {
      return { content : 'reference missing', mdid : object.source };
    } else if (object.useProperty)
      var referencedValue = source[object.useProperty.toLowerCase()];
    else
      console.warn("!! no useProperty for", object);
    // console.log(referencedValue);
    return { content : referencedValue, editable : true, mdid :  source.mdid, property: object.useProperty };
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
  var blankContent = !value.content || value.content === "" || value.content.match(/^\s+$/);
  if (blankContent) {
    classes.push('blank')
  }
  if (value.editable) {
    classes.push('editable');
    // TODO use something other than id here, since the same reference can appear multiple times
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
    child.viewData.modifiedFormatted = app.formatDate(parseDate(child.viewData.modified));

    // console.log("contains:", child.viewData.contains);
    for (var cIdx in child.viewData.contains) {
      
      var c = child.viewData.contains[cIdx];
      if (c.type == 'Table') {
        // console.log("skipping table...");
        var table = '<table class="table table-striped">';
        table += "<thead>";
        table += "<tr>";
        for (var hIdx in c.header[0]) {
          var cell = c.header[0][hIdx];
          var value = resolveValue(cell.content, elements, function(valueList) {
            return _.map(valueList, function(v) { return renderEmbeddedValue(v, elements) }).join("");
          });
          // console.log("header value", value)
          table += '<th colspan="'+ (cell.colspan || 1) + '" rowspan="' + (cell.rowspan || 1) + '">' + value + "</th>";
        }
        table += "</tr>";
        table += "</thead>"
        table += "<tbody>";
        for (var rIdx in c.body) {
          table += "<tr>";
          for (var cIdx in c.body[rIdx]) {
            var cell = c.body[rIdx][cIdx];
            var value = resolveValue(cell.content, elements, function(valueList) {
              return _.map(valueList, function(v) { return renderEmbeddedValue(v, elements) }).join(", ");
            });
            // TODO need to pull out the renderer here so that we can do multiple divs in a cell
            table += '<td colspan="'+ (cell.colspan || 1) + '" rowspan="' + (cell.rowspan || 1) + '">' + value + "</td>";
          }
          table += "</tr>";
        }
        table += "</tbody>"
        table += "</table>"
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
  // index views by id
  var viewsById = {};
  for (var idx in viewData.views) {
    var view = viewData.views[idx];
    viewsById[view.mdid] = view;
  }
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
  viewTree.snapshots = viewData.snapshots;

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

window.initializeSvgHandler = function($el) {
  // console.log("initializing svg edit handler for", $el);
  $el.attr('contenteditable', false).click(function() {
    // console.log("regular jquery click handler ", this);
    // console.log("click event", evt);
    var $svg = $(this);
    $svg.after('<div class="editor" contenteditable="false"></div>');
    var $editorContainer = $svg.next('.editor')
    $editorContainer.append('<div class="mini-toolbar"><button class="btn btn-default btn-sm" onclick="deleteSvg(this)">delete</button><div class="pull-right btn-group"><button class="btn btn-default btn-sm" type="button" onclick="cancelSvg(this)">Cancel</button><button class="btn btn-default btn-sm" type="button" onclick="saveSvg(this)">Save</button></div></div>');
    $editorContainer.append('<iframe src="vendor/svgedit/svg-editor.html" width="100%" height="600px" onload="init_embed(this)"></iframe>');
    
    // set initial content (innerHTML and outerHTML don't work with svgs)
    var serializer = new XMLSerializer();
    var svgString = serializer.serializeToString(this);

    // save this as an attribute for later loading
    $editorContainer.find('iframe').attr('data-original-svg', svgString);
    // console.log("set initial svg to", svgString);
    // console.log("set initial svg to", '<svg>'+$svg.html()+'</svg>', this);
    $svg.hide();
  });  
  return $el;
}

var cancelEditing = function(container) {
  $(container).remove();
}

window.saveSvg = function(btn) {
  // console.log("clicked", btn);
  var $editor = $(btn).closest('.editor');
  // console.log("editor", $editor);
  // console.log("frame", $editor.find('iframe'));
  var canvas = $editor.find('iframe').data('canvas');
  // console.log("canvas", canvas);
  canvas.getSvgString()(function(data, error) {
    if (error) {
      app.fire('message', 'error', error);
    } else {
      // console.log("new svg data", data);
      $editor.prev('svg').replaceWith(data);
      initializeSvgHandler($editor.prev('svg'));
      $editor.remove();
    }
  });
}

window.deleteSvg = function(btn) {
  var $editor = $(btn).closest('.editor');
  $editor.prev('svg').remove();
  $editor.remove();
}

window.cancelSvg = function(btn) {
  console.log("clicked", btn);
  var $editor = $(btn).closest('.editor');
  $editor.prev('svg').show();
  $editor.remove();
}

window.init_embed = function(frame) {
  var svgCanvas = new embedded_svg_edit(frame);
  $(frame).data('canvas', svgCanvas);
    
  // Hide main button, as we will be controlling new/load/save etc from the host document
  var doc;
  doc = frame.contentDocument;
  if (!doc)
  {
    doc = frame.contentWindow.document;
  }
    
  var mainButton = doc.getElementById('main_button');
  mainButton.style.display = 'none';

  var originalSvg = $(frame).attr('data-original-svg');
  // console.log("loading original svg content:", originalSvg);
  svgCanvas.setSvgString($(frame).attr('data-original-svg'));
}
    
app.on('initializeSvgEditor', function(el) {

  initializeSvgHandler($(el).find('svg'));

  // TODO strip contenteditable attribute before saving
})

// FIXME this wasn't getting triggered
app.on('insertSvg', function() {
  console.log("!! inserting svg !!");
})

window.insertSvg = function() {
  document.execCommand('insertHTML', false, '<svg class="new-svg" width="640" height="480" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg"><g><circle cx="0" cy="0" r="0" fill="white" /></g></svg>');
  $svg = $('svg.new-svg').removeClass('new-svg');
  // console.log("new svg element:", $svg);
  initializeSvgHandler($svg).click(); // attach and trigger immediately
} 

// toc.js

app.on('makeToc', function() {
  $("#toc").tocify({ selectors: "h2, h3, h4, h5", history : false, scrollTo: "60", hideEffect: "none", showEffect: "none", highlightOnScroll: true, highlightOffset : 0, context: "#the-document", smoothScroll:false, extendPage:false }).data("toc-tocify");  
})

</script>
</body>
</html>