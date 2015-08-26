import json

# YOU POST THIS FIRST
TEMPLATE_NEW_VIEW = json.dumps({
    "elements": [
        {
            "specialization": {
                "type": "View"
            },
            "owner": "OWNER_ID",
            "name": "VIEW_NAME",
            "documentation": ""
        }
    ]
})
# POST THIS SECOND
TEMPLATE_INSTANCE_SPEC = json.dumps({
    "elements":[
        {
            "name": "View Documentation",
            "specialization":{
                "type":"InstanceSpecification",
                "classifier":[
                    "_17_0_5_1_407019f_1431903758416_800749_12055"
                ],
                "instanceSpecificationSpecification":{
                    "string":{"type": "Paragraph",
                               "sourceType": "reference",
                               "source":"VIEW_ID",
                               "sourceProperty":"documentation"},
                    "type": "LiteralString"
                }
            },
        "owner":"test-site_no_project"
        }
    ]
})





# POST THIS TO UPDATE THE VIEW ON SERVER AFTER GETTING INSTANCE SPECIFICATION
TEMPLATE_NEW_VIEW_WITH_INSTANCE_SPEC = json.dumps({
    "elements": [
        {
            "sysmlid": "VIEW_ID",
            "specialization":{
                "type": "View",
                "allowedElements": [],
                "displayedElements":[],
                "childrenViews": [],
                "contents": {
                    "operand": [
                        {
                            "instance":"SYSMLID_FROM_RESPONSE_JSON",
                            "type":"InstanceValue"
                        }
                    ],
                    "type":"Expression"
                }
            }
        }
    ]
})

# POST THIS TO UPDATE THE PARENT PRODUCT
TEMPLATE_POST_PARENT_PRODUCT = json.dumps({
    "elements": [
        {
            "sysmlid": "",
            "specialization":{
                "type":"",
                "allowedElements":[
                    ""
                ],
                "displayedElements":[
                    ""
                ],
                "view2view":[
                    {
                        "id":"",
                        "childrenViews":[
                            ""
                        ]
                    },
                    {
                        "id":"",
                        "childrenViews":[]
                    }
                ]
            }
        }
    ]
})

TEMPLATE_TYPE_PRODUCT = json.dumps({"type": "Product"})
TEMPLATE_ALLOWED_ELEMENTS = json.dumps({"allowedElements": []})
TEMPLATE_DISPLAYED_ELEMENTS = json.dumps({"displayedElements": []})
TEMPLATE_VIEW2VIEW_NEW_VIEW = json.dumps({"view2view": [{"id": "", "childrenViews": []}]})
