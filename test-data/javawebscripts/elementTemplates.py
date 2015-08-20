import json

# YOU POST THIS FIRST
TEMPLATE_NEW_VIEW = json.loads({
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
TEMPLATE_INSTANCE_SPEC = json.loads({
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

# GET DOCUMENT OR PARENT VIEW TO REFRESH PAGE ON WEBAPP
NEW_WEB_PAGE_INSTANCE_SPEC = json.loads({
    "elements": [
        {
            "name": "View Documentation",
            "specialization": {
                "type": "InstanceSpecification",
                "classifier": [
                    "_17_0_5_1_407019f_1431903758416_800749_12055"
                ],
                "instanceSpecificationSpecification": {
                    # VIEW_ID IS THE SAME AS NEW INSTANCE SPEC ABOVE
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
NEW_VIEW_ELEMENT = json.loads({
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
TEMPLATE_PRODUCT = json.loads({
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

# CALL THIS TO GET THE NEW PARENT PRODUCT
GET_NEW_PRODUCT = json.loads({
    "elements": [
        {
            "sysmlid": "MMS_1440004666224_40c9834a-d6d0-47ff-bfe3-54d25e0462d0",
            "specialization": {
                "type": "Product",
                "allowedElements": [
                    "MMS_1440004666224_40c9834a-d6d0-47ff-bfe3-54d25e0462d0"
                ],
                "displayedElements": [
                    "MMS_1440004666224_40c9834a-d6d0-47ff-bfe3-54d25e0462d0"
                ],
                "view2view": [
                    {
                        "id": "MMS_1440004666224_40c9834a-d6d0-47ff-bfe3-54d25e0462d0",
                        "childrenViews": [
                            "MMS_1440004700246_e9451e3f-f060-4af0-a452-9cef660b3fa4"
                        ]
                    },
                    {
                        "id": "MMS_1440004700246_e9451e3f-f060-4af0-a452-9cef660b3fa4",
                        "childrenViews": []
                    }
                ]
            }
        }
    ]
})
TEMPLATE_TYPE_PRODUCT = json.loads({"type": "Product"})
TEMPLATE_ALLOWED_ELEMENTS = json.loads({"allowedElements": []})
TEMPLATE_DISPLAYED_ELEMENTS = json.loads({"displayedElements": []})
VIEW2VIEW_TEMP = json.loads({"view2view": [{"id": "", "childrenViews": []}]})
