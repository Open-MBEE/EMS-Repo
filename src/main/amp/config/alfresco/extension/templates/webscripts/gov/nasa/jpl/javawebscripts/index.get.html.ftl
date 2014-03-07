<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=edge;chrome=1" />
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<title>EMS View Editor: ${title}</title>
		<link rel="stylesheet" href="${url.context}/scripts/vieweditor/vendor/css/bootstrap.min.css" media="screen">
		<link href="${url.context}/scripts/vieweditor/styles/jquery.tocify.css" rel="stylesheet" media="screen">
		<link href="${url.context}/scripts/vieweditor/styles/styles.css" rel="stylesheet" media="screen">
		<link href="${url.context}/scripts/vieweditor/styles/print.css" rel="stylesheet" media="print">
		<link href="${url.context}/scripts/vieweditor/styles/fonts.css" rel="stylesheet">
		<link href="${url.context}/scripts/vieweditor/styles/section-numbering.css" rel="stylesheet">
		<link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>
	
<script type="text/javascript">
var pageData = { home: ${res},  baseUrl: "${url.context}/service" };
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
						<a class="navbar-brand" href="${url.context}/service/ve/documents/${siteName}">${siteTitle} View Editor {{title}}</a>
					{{/environment.development}}
			</div>
			<ul class="nav navbar-nav">
				<li><a href="/share/page/">EMS Dashboard</a></li>
			</ul>


			<div class="pull-right">
				<img class="europa-icon" src="${url.context}/scripts/vieweditor/images/europa-icon.png" />
			</div>

	      <ul class="nav navbar-nav pull-right">
       		<li><a href="#" class="submit-logout">logout</a></li>
	      </ul>

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
			<div class="row">
  
  <div class="col-md-4">
    
    <div class="panel panel-default">
      <div class="panel-heading">{{homeTree.name}}</div>
      <div class="panel-body">
        <ul>
          {{#homeTree.children}}
            {{>doc_and_children}}
          {{/homeTree.children}}
        </ul>
      </div>
    </div>

    <!-- {{>doc_and_children}} -->
      {{^hidden}}
        <li class="{{ .class }}">
          {{#showLink}}
          <a href="${url.context}/service/ve/products/{{id}}">{{name}}</a>
          {{/showLink}}
          {{^showLink}}
          {{ name }}
          {{/showLink}}
        </li>
        <ul>
          {{#.children}}
            {{>doc_and_children}}
          {{/.children}}
        </ul>
      {{/hidden}}
    <!-- {{/doc_and_children}} -->


    {{#(settings.currentWorkspace != 'modeler')}}
<!--       <div class="panel">
        <div class="panel-heading">Tips and tricks</div>
        <ul class="list-group">
          <li class="list-group-item">
            Things go here
          </li>
        </ul>
      </div>
 -->    {{/()}}

  </div>

<!--   <div class="col-md-4">
    
    {{#(settings.currentWorkspace != 'reviewer')}}
    <div class="panel">
      <div class="panel-heading">My documents</div>
      <ul class="list-group">
        <li class="list-group-item">
          <small><span class="pull-right text-muted">yesterday at 4:22 pm</span></small>
          <a href="plan.html">Europa System Engineering Management Plan</a>
        </li>
      </ul>
    </div>
    {{/()}}

    <div class="panel">
      <div class="panel-heading">Pending review</div>
      <ul class="list-group">
        <li class="list-group-item">
          <small><span class="pull-right text-muted">yesterday at 4:22 pm</span></small>
          <a href="plan.html">Europa System Engineering Management Plan</a>
        </li>
      </ul>
    </div>


  </div>

  <div class="col-md-4">
    
    {{#(settings.currentWorkspace === 'modeler')}}
    <div class="panel">
      <div class="panel-heading">Recent comments</div>
      <ul class="list-group">
          <li class="list-group-item">
            <small><span class="pull-right text-muted">a few minutes ago</span></small>
            <a href="plan.html">Some comment</a>
          </li>
          <li class="list-group-item">
            <small><span class="pull-right text-muted">yesterday at 10:18 am</span></small>
            <a href="plan.html">Another comment</a>
          </li>
      </ul>
    </div>
    {{/()}}

    <div class="panel">
      <div class="panel-heading">Recent changes</div>
      <ul class="list-group">
        <li class="list-group-item">
          <small><span class="pull-right text-muted">yesterday at 4:22 pm</span></small>
          <a href="plan.html">Europa System Engineering Management Plan</a>
        </li>
      </ul>
    </div>

  </div>

</div>
		</div>

		
		
		<!--  -->
		
		
		
		
		
		
	</script><script src="${url.context}/scripts/vieweditor/vendor/jquery.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery-ui.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.hotkeys.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/bootstrap-wysiwyg.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.tocify.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/underscore.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/moment.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/bootstrap.min.js"></script>
<script type="text/javascript" src="${url.context}/scripts/vieweditor/vendor/Ractive.js"></script>
<script type="text/javascript">
$(document).ready(function() {
	$('a.submit-logout').click(function() {
		window.location.replace('${url.context}/service/logout/info?next=${url.full}');
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
    .done(function() { app.fire('message', 'success', successMessage); })
    .fail(function(e) { 
      app.fire('message', 'error', errorMessage); 
      if (console && console.log) {
        console.log("ajax error:", e);
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
    contentType: "application/json; charset=UTF-8"
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

app.on('togglePreview', function() {
	// console.log("toggling preview...");
	$('#markup-preview, #markup-editor').toggle();
})

app.on('editSection', function(e, sectionId) {

  e.original.preventDefault();


  // app.set('oldData.'+sectionId, app.generateUpdates);
  // console.log("editing a section!", e, sectionId);
  // TODO turn editing off for all other sections
  app.set(e.keypath+'.editing', true);
  var section = $("[data-section-id='" + sectionId + "']");
  // TODO make this work with multiple tables in a section
  section.each(function(i,el) {
    $(el).wysiwyg();
  });
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

  // app.createLiveTable($('.rich-table'));
  // app.set(e.keypath+'.previousContent', app.get(e.keypath+'.content'));
  // console.log("saved current content to previous content", app.get('keypath'));

  // handle placeholder text
  // TODO remove this listener on cancel or save
  section.click(function() {
    var $el = $(app.getSelectedNode());
    if ($el.is('.editable.reference.blank')) {
      $el.html('&nbsp;');
      $el.removeClass('blank');
    }
  });

  // make sneaky overlay for image uploads
  $('[data-role=magic-overlay]').each(function () {
    var overlay = $(this), target = $(overlay.data('target')); 
    overlay.css('opacity', 0).css('position', 'absolute').offset(target.offset()).width(target.outerWidth()).height(target.outerHeight());
  });


})

app.on('cancelEditing', function(e) {
  e.original.preventDefault();
  app.set(e.keypath+'.editing', false);
  // app.set(e.keypath+'.content', app.get(e.keypath+'.previousContent'));
  console.log("canceled", app.get(e.keypath));
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

app.data.printPreviewTemplate = "\n<html>\n\t<head>\n\t\t<title>{{ documentTitle }}</title>\n\t\t<link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>\n\t\t<style type=\"text/css\">\n\n\n\t\t  .no-section {\n\t\t    display: none;\n\t\t  }\n\n\t\t  .page-sections.no-section {\n\t\t    display: block;\n\t\t  }\n\n\t\t  .blank.reference {\n\t\t\t  display: none;\n\t\t\t}\n\n\t\t\t@page {\n\t\t\t  margin: 1cm;\n\t\t\t}\n\n\t\t\tbody {\n\t\t\t  font-family: 'Source Sans Pro', Helvetica, Arial, sans-serif;\n\t\t\t}\n\n\t\t\t.page-sections, #the-document h1, #the-document h2, #the-document h3, #the-document h4, #the-document h5 {\n\t\t\t  font-family: 'PT Serif', Georgia, serif;\n\t\t\t}\n\t\t\t.navbar-brand, .page .inspector, .inspectors {\n\t\t\t  font-family: 'Source Sans Pro', Helvetica, Arial, sans-serif;\n\t\t\t}\n\n\t\t\t#the-document {counter-reset: level1;}\n\t\t\t#toc:before, #toc:after {counter-reset: level1; content: \"\";}\n\t\t\t#toc h3:before{content: \"\"}\n\t\t\t \n\t\t\t#the-document h1, #toc > ul > li {counter-reset: level2;}\n\t\t\t#the-document h2, #toc > ul >  ul > li {counter-reset: level3;}\n\t\t\t#the-document h3, #toc > ul > ul > ul > li {counter-reset: level4;}\n\t\t\t#the-document h4, #toc > ul > ul > ul > ul > li {counter-reset: level5;}\n\t\t\t#the-document h5, #toc > ul > ul > ul > ul > ul > li {}\n\n\t\t\t#the-document h1:before,\n\t\t\t#toc > ul > li a:before {\n\t\t\t    content: counter(level1) \" \";\n\t\t\t    counter-increment: level1;\n\t\t\t}\n\t\t\t#the-document h2:before,\n\t\t\t#toc > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \" \";\n\t\t\t    counter-increment: level2;\n\t\t\t}\n\t\t\t#the-document h3:before,\n\t\t\t#toc > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \" \";\n\t\t\t    counter-increment: level3;\n\t\t\t}\n\t\t\t#the-document h4:before,\n\t\t\t#toc > ul > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \".\" counter(level4) \" \";\n\t\t\t    counter-increment: level4;\n\t\t\t}\n\t\t\t#the-document h5:before,\n\t\t\t#toc > ul > ul > ul > ul > ul > li a:before {\n\t\t\t    content: counter(level1) \".\" counter(level2) \".\" counter(level3) \".\" counter(level4) \".\" counter(level5) \" \";\n\t\t\t    counter-increment: level5;\n\t\t\t}\n\t\t</style>\n\t</head>\n\t<body>\n\t\t<div id=\"the-document\">\n\t\t\t{{ content }}\n\t\t</div>\n\t</body>\n</html>";

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
      node.showLink = !(node.id in homeData.volumes); //node.children.length == 0;
      node.hidden = node.name === 'Unexported Document';
      buildHomeTree(node.children)
    })
  }
  buildHomeTree(homeTree.children)
  console.log("final home tree", homeTree)
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

  // // app.set('debug', JSON.stringify(viewData));
  // console.log(viewData);
  // TODO: Change addChildren to construct the parentNode content instead of the childrenNodes
  // Then we could just pass viewTree instead of tempTree
  var tempTree = {"children" : []};
  addChildren(tempTree, [viewData.rootView], viewData.view2view, viewData.views, elementsById, 0);
  // console.log("tempTree", tempTree);
  
  viewTree = tempTree.children.length > 0 ? tempTree.children[0] : [];
  viewTree.orderedChildren = constructOrderedChildren(viewTree);

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

// toc.js

app.on('makeToc', function() {
	$("#toc").tocify({ selectors: "h1, h2, h3, h4", history : false, highlightOffset : 0, context: "#the-document", smoothScroll:false }).data("toc-tocify");	
})

</script>
</body>
</html>
