<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>memos</title>
    <link rel="stylesheet" href="https://netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css">
    <link href="${url.context}/scripts/vieweditor/styles/jquery.tocify.css" rel="stylesheet">
    <link href="${url.context}/scripts/vieweditor/styles/styles.css" rel="stylesheet">
    <link href="${url.context}/scripts/vieweditor/styles/fonts.css" rel="stylesheet">
    <link href='https://fonts.googleapis.com/css?family=Source+Sans+Pro|PT+Serif:400,700' rel='stylesheet' type='text/css'>
  
<script type="text/javascript">
var pageData = {postview: ${res}};
</script>

</head>

  <body class="{{ meta.pageName }} {{ settings.currentWorkspace }}">
<div id="main"></div>
<script id="template" type="text/mustache">

    <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="navbar-header">
          <a class="navbar-brand" href="/">Europa View Editor {{ title }}</a>
      </div>
      <ul class="nav navbar-nav">
        <li><a href="index.html">dashboard</a></li>
        <li><a href="about.html">about</a></li>
      </ul>


      <div class="pull-right">
        <a href="vision.html"><img class="europa-icon" src="images/europa-icon.png" /></a>
      </div>

      <form class="navbar-form navbar-right" action="">
        <div class="form-group">
          <select id="workspace-selector" class="form-control input-sm" value="{{ settings.currentWorkspace }}">
            <option value="modeler">Modeler</option>
            <option value="reviewer">Reviewer</option>
            <option value="manager">Manager</option>
          </select>
        </div>
      </form>

    </nav>

    <div class="wrapper">
      <div class="row split-view">

  <div class="col-xs-8">
    <div class="page-sections">
      {{#viewTree.children}}
      <div class="section-wrapper">
        <div class="section-actions pull-right btn-group">
          {{^editing}}
          <a href="#" class="btn btn-primary btn-sm">comment</a>
          <a href="#" class="btn btn-primary btn-sm" proxy-click="editSection:{{ id }}">edit</a>
          {{/editing}}
        </div>
        {{#editing}}
        <div class="toolbar page">
          <div class="btn-group">
            <button type="button" class="btn btn-default" proxy-click="insertTable">Insert table</button>
            <button type="button" class="btn btn-default" proxy-click="insertReference">Insert reference</button>
            <!-- <button type="button" class="btn btn-default">Insert equation</button> -->
          </div>
          <div class="btn-group pull-right">
            <button type="button" class="btn btn-default" proxy-click="cancelEditing">Cancel</button>
            <button type="button" class="btn btn-primary" proxy-click="saveSection:section{{ id }}">Save changes</button>
          </div>
        </div>
        <div class="section page" id="section{{ id }}" contenteditable="true" proxy-dblclick="sectionDoubleClick">
          {{{ content }}}
        </div>
        {{/editing}}
        {{^editing}}
        <div class="section page" id="section{{ id }}">
          {{{ content }}}
        </div>
        {{/editing}}
         <div class="comments">
          {{#comments}}
            {{#comments}}
              <div class="comment">{{ content }} &mdash; {{ author }}</div>
            {{/comments}}
          <div class="comment-form" style="display:none">
            <br/>
            <textarea value="{{ newComment }}"></textarea>
            <br/><br/>
            <a href="#" class="btn btn-primary" proxy-click="addComment">Add comment</a>
          </div>
          {{/comments}}
        </div>
      </div>
      {{/viewTree.children}}
    </div>
  </div>

  <div class="col-xs-4">
    <div class="toggled-inspectors inspectors affix page col-xs-4">

      <select class="form-control" value="{{ currentInspector }}">
        <option value="document-info">Document info</option>
        <option value="history">History</option>
        <option value="references">References</option>
      </select>

      <div id="document-info" class="inspector">
        <h3>Document info</h3>
        <dl>
          <dt>Author</dt><dd>Chris Delp</dd>
          <dt>Last modified</dt><dd>8/14/13 2:04pm</dd>
        </dl>
        <div id="toc"></div>
      </div>

      <div id="history" class="inspector">
        <h3>History</h3>
        <ul class="list-unstyled">
          <li>v1 &mdash; Chris Delp</li>
        </ul>
      </div>

      <div id="comments" class="inspector">
        <h3>Comments</h3>
        <div class="comments">
          {{#comments}}
            {{#comments}}
              <div class="comment">{{ content }} &mdash; {{ author }}</div>
            {{/comments}}
          <br/>
          <textarea value="{{ newComment }}" class="form-control col-xs-3"></textarea>
          <a href="#" class="btn btn-primary" proxy-click="addComment">Add comment</a>
          {{/comments}}
        </div>
      </div>

      <div id="references" class="inspector">
        <h3>References</h3>
        <ul>
          {{#postview.elements}}
          <li>
             {{ name }}
          </li>
          {{/postview.elements}}
        </ul>
      </div>


    </div>
  </div>


</div>
    </div>

    
    
    <!--  -->
    
    
    
  </script><script src="${url.context}/scripts/vieweditor/vendor/jquery.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery-ui.min.js"></script>
<script src="http://netdna.bootstrapcdn.com/twitter-bootstrap/2.3.1/js/bootstrap.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.hotkeys.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/bootstrap-wysiwyg.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/jquery.tocify.min.js"></script>
<script src="${url.context}/scripts/vieweditor/vendor/underscore.js"></script>
<script type="text/javascript" src="${url.context}/scripts/vieweditor/vendor/Ractive.js"></script>
<script type="text/javascript">var app = new Ractive({ el : "main", template : "#template", data : pageData });</script>
<script type="text/javascript">
var context = window;

// comments.js

app.on('addComment', function(evt) {
  app.get(evt.keypath).comments.push({ author : 'Jesse', content : app.get('newComment')});
  app.set('newComment','');
  evt.original.preventDefault();
});

// editor.js

var $ = context.$;

// console.log("$ is currently", $);

setTimeout(function() {
  context.$('#editor').wysiwyg(); 
}, 500)

app.on('togglePreview', function() {
  // console.log("toggling preview...");
  $('#markup-preview, #markup-editor').toggle();
})

app.on('editSection', function(e, sectionId) {
  e.original.preventDefault();
  // console.log("editing a section!", e, sectionId);
  // TODO turn editing off for all other sections
  app.set(e.keypath+'.editing', true);
  // TODO make this work with multiple tables in a section
  app.createLiveTable($('.rich-table'));
  // app.set(e.keypath+'.previousContent', app.get(e.keypath+'.content'));
  // console.log("saved current content to previous content", app.get('keypath'));
})

app.on('cancelEditing', function(e) {
  e.original.preventDefault();
  app.set(e.keypath+'.editing', false);
  // app.set(e.keypath+'.content', app.get(e.keypath+'.previousContent'));
  console.log("canceled", app.get(e.keypath));
})

app.on('saveSection', function(e, sectionId) {
  e.original.preventDefault();
  app.set(e.keypath+'.content', context.document.getElementById(sectionId).innerHTML);
  app.set(e.keypath+'.editing', false);
})

app.on('insertTable', function(e) {
  // TODO how do we initialize this ractive for existing content?
  var tableData = [
    ['Header 1', 'Header 2'],
    ['Value 1', 'Value 2']
  ];

  // var tableContent = '<table class="table table-bordered table-striped"><tr><td>your stuff here</td></tr></table>';
  context.document.execCommand('insertHTML', false, '<div id="tableTest" class="rich-table">table goes here</div>');
  var liveTable = app.createLiveTable($('#tableTest'), tableData);
  liveTable.set('editing', true);

})

app.on('insertReference', function() {
  context.document.execCommand('insertHTML', false, '<div id="referenceTest" class="rich-reference">reference goes here</div>');
  var liveReference = app.createLiveReference($('#referenceTest'), {}, app.get('elements'));
  liveReference.set('editing', true);
})

// elementDetails.js

app.on('elementDetails', function(evt) {
  evt.original.preventDefault();
  app.set('inspectedElement', app.get(evt.keypath));
  evt.node.blur();
})

// inspectors.js

var $ = context.$;

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

var $ = context.$;

var viewTree = {

}



var generateUpdates = function(node)
{
   var result = [];
  $('.editable[property]', node).each(function(i,el)
  {
    //$('div[property]', editableNode).each(function(i,el)
    //{
      var existsInResults = false;
      _.each(result, function(x)
      {
        if(x.mdid === el.id)
        {
          existsInResults = true;
        }
      });
      if(existsInResults === false)
      {
        result.push({
          mdid: el.id,
          documentation: el.innerHTML
        })
      }
    //});
  });
  return result;
}

var saveData = function(viewID, updates)
{
  var data = {'posted content': updates}
  console.log("Write updates here", data);
  var jsonData = JSON.stringify(updates);
  var url = '${url.context}/service/ui/views/' + viewID;
  $.ajax(
    { 
      type: "POST",
      url: url,
      data: jsonData,
      contentType: "application/json; charset=UTF-8",
      success: function(r) {
        console.log("Success writing back");
      }
    })
  .fail(function() { console.log("Error writing back"); });
}

var writeBackCache = function()
{
   var elementsToWriteback = [];
   var viewData = {};
   app.observe('postview', function(vd) {
     viewData = vd;
   })

   app.on('saveSection', function(e, sectionId) {   
     //console.log("realData SaveSection " + sectionId)
     var section = context.document.getElementById(sectionId);

     var updates = generateUpdates(section);
     var viewId = sectionId.replace('section', '');
     saveData(viewId, updates);
   })
}();

var resolveValue = function(object, elements) {
  if (object.source === 'text') {
    return { content : object.text, editable : false };
  } else {
    console.log("resolving ", object.useProperty, " for ", object.source);

    var source = elements[object.source];
    console.log(source);
    var referencedValue = source[object.useProperty.toLowerCase()];
    console.log(referencedValue);
    return { content : referencedValue, editable : true, mdid :  source.mdid, property: object.useProperty };
  }
}


var addChildren = function(parentNode, childIds, view2view, views, elements) {
  // console.log("iterating through", childIds);
  // return;
  for (var idx in childIds) {
    var id = isNaN(idx) ? idx : childIds[idx];
    // console.log("handing", id);
    var child = { id : id, children: [], viewData : app.get('viewsById')[id] };
    // resolve referenced content
    child.content = "";

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
          // TODO use the same resolver code here
          var value = resolveValue(cell, elements);
          table += '<th colspan="'+ (cell.colspan || 1) + '" rowspan="' + (cell.rowspan || 1) + '"' + "><div property='" + value.property + "' id='" + value.mdid +"'>" + value.content + "</div></th>";
        }
        table += "</tr>";
        table += "</thead>"
        table += "<tbody>";
        for (var rIdx in c.body) {
          table += "<tr>";
          for (var cIdx in c.body[rIdx]) {
            var cell = c.body[rIdx][cIdx];
            var value = resolveValue(cell, elements);
            table += '<td colspan="'+ (cell.colspan || 1) + '" rowspan="' + (cell.rowspan || 1) + '"' + "><div property='" + value.property + "' id='" + value.mdid +"'>" + value.content + "</div></td>";
          }
          table += "</tr>";
        }
        table += "</tbody>"
        table += "</table>"
        child.content += table;
      } else {
        var value = resolveValue(c, elements);
        if (value.editable) {
          child.content += '<div class="editable" property="' + value.property + '" id="' + value.mdid +'">';
        } else {
          var ref = elements[value.mdid];
          if (ref) {
            child.content += '<div class="reference" contenteditable="false" title="'+ref.name+' ('+c.useProperty.toLowerCase()+')'+'">';          
          } else {
            child.content += '<div class="missing">reference missing</div>'
          }
        }
        child.content += value.content;
        child.content += '</div>';
      }
    }
    
    if (view2view[id]) {
      addChildren(child, view2view[id], view2view, views, elements)
    }
    parentNode.children.push(child);
    // console.log("added", child, " to parent ", parentNode);
  }
}

app.observe('postview', function(viewData) {
  // index views by id
  var viewsById = {};
  for (var idx in viewData.views) {
    var view = viewData.views[idx];
    viewsById[view.mdid] = view;
  }
  console.log('viewsById', viewsById);
  app.set('viewsById', viewsById);
  // index elements by id
  var elementsById = {};
  for (var idx in viewData.elements) {
    var e = viewData.elements[idx];
    elementsById[e.mdid] = e;
  }
  app.set('elementsById', elementsById);

  // // app.set('debug', JSON.stringify(viewData));
  console.log(viewData);
  // TODO: Change addChildren to construct the parentNode content instead of the childrenNodes
  // Then we could just pass viewTree instead of tempTree
  var tempTree = {"children" : []};
  addChildren(tempTree, [viewData.rootView], viewData.view2view, viewData.views, elementsById);
  viewTree = tempTree.children[0];
  console.log("final view tree", viewTree); 
  app.set('viewTree', viewTree);
})

// rich-reference.js

var $ = context.$;

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

var $ = context.$;

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

var $ = context.$;

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

context.$("#toc").tocify({ selectors: "h2, h3, h4", history : false, highlightOffset : 0 }).data("toc-tocify");
</script>
</body>
</html>