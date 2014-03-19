ViewEditor.Document = DS.Model.extend({
   title: DS.attr('string'),

});

ViewEditor.Document.FIXTURES = [
    {
      "title": "The best document evers?",
      id: "1"
    }
];