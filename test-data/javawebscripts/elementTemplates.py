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
    "elements": [
        {
            "name": "View Documentation",
            "specialization": {
                "type": "InstanceSpecification",
                "classifier": [
                    "_17_0_5_1_407019f_1431903758416_800749_12055"
                ],
                "instanceSpecificationSpecification": {
                    # THIS CONTAINS THE NEW VIEW ID
                    "string": {"type": "Paragraph",
                               "sourceType": "reference",
                               "source": "VIEW_ID",
                               "sourceProperty": "documentation"},
                    "type": "LiteralString"
                }
            },
            "owner": "test-site_no_project"
        }
    ]
})

# POST THIS TO UPDATE THE VIEW ON SERVER AFTER GETTING INSTANCE SPECIFICATION
NEW_VIEW_ELEMENT = json.dumps({
    "elements": [
        {
            # VIEW_ID IS THE SAME AS NEW INSTANCE SPEC ABOVE
            "sysmlid": "VIEW_ID",
            "specialization": {
                "type": "View",
                "allowedElements": [
                    "MMS_1440004700246_e9451e3f-f060-4af0-a452-9cef660b3fa4"
                ],
                "displayedElements": [
                    "MMS_1440004700246_e9451e3f-f060-4af0-a452-9cef660b3fa4"
                ],
                "childrenViews": [],
                "contents": {
                    "operand": [
                        {
                            "instance": "MMS_1440004700481_0519cab9-ee4f-4846-a9dc-e04510318196",
                            "type": "InstanceValue"
                        }
                    ],
                    "type": "Expression"
                }
            }
        }
    ]
})

# POST THIS TO UPDATE THE PARENT PRODUCT
TEMPLATE_PRODUCT = json.dumps({
    "elements": [
        {
            "sysmlid": "",  # PRODUCT SYSMLID
            "specialization": {
                "type": "Product",
                "allowedElements": [
                    ""  # PRODUCT SYSMLID
                ],
                "displayedElements": [
                    ""  # PRODUCT SYSMLID
                ],
                "view2view": [
                    {
                        "id": "",  # PRODUCT SYSMLID
                        "childrenViews": [
                            ""  # CHILD SYSMLID
                        ]
                    },
                    {
                        "id": "",  # CHILD SYSMLID
                        "childrenViews": []
                    }
                ]
            }
        }
    ]
})

TEMPLATE_TYPE_PRODUCT = json.dumps({"type": "Product"})
TEMPLATE_ALLOWED_ELEMENTS = json.dumps({"allowedElements": []})
TEMPLATE_DISPLAYED_ELEMENTS = json.dumps({"displayedElements": []})
VIEW2VIEW_TEMP = json.dumps({"view2view": [{"id": "", "childrenViews": []}]})
