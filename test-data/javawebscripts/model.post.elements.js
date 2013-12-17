{ "elementHierarchy" : { "_12345_0_be00301_1147682946132_981479_8" : [ "_12345_0_be00301_1147682950728_698804_11",
          "_12345_0_be00301_1147682986961_237223_97",
          "_12345_0_be00301_1147683150866_622773_103"
        ]
    },
  "elements" : { "_12345_0_be00301_1147682946132_981479_8" : { "documentation" : "A FlowPort is an interaction point through which input and/or output of items such as data, material, or energy may flow. This enables the owning block to declare which items it may exchange with its environment and the interaction points through which the exchange is made. We distinguish between atomic flow port and a nonatomic flow port. Atomic flow ports relay items that are classified by a single Block, ValueType, DataType, or Signal classifier. A nonatomic flow port relays items of several types as specified by a FlowSpecification. Flow ports and associated flow specifications define “what can flow” between the block and its environment, whereas item flows specify “what does flow” in a specific usage context. Flow ports relay items to their owning block or to a connector that connects them with their owner’s internal parts (internal connector).",
          "isView" : false,
          "name" : "FlowPort",
          "owner" : "_17_0_3_17530432_1320212535488_356435_2067",
          "type" : "Element"
        },
      "_12345_0_be00301_1147682950728_698804_11" : { "documentation" : "",
          "isDerived" : false,
          "isSlot" : false,
          "isView" : false,
          "name" : "base_Port",
          "owner" : "_12345_0_be00301_1147682946132_981479_8",
          "type" : "Property"
        },
      "_12345_0_be00301_1147682986961_237223_97" : { "documentation" : "Indicates the direction in which an atomic flow port relays its items. If the direction is set to “in,” then the items are relayed from an external connector via the flow port into the flow port’s owner (or one of its parts). If the direction is set to “out,” then the items are relayed from the flow port’s owner, via the flow port, through an external connector attached to the flow port. If the direction is set to “inout,” then items can flow both ways. By default, the value is inout.",
          "isDerived" : false,
          "isSlot" : false,
          "isView" : false,
          "name" : "direction",
          "owner" : "_12345_0_be00301_1147682946132_981479_8",
          "type" : "Property"
        },
      "_12345_0_be00301_1147683150866_622773_103" : { "documentation" : "This is a derived attribute (derived from the flow port’s type). For a flow port typed by a flow specification the value of this attribute is False, otherwise the value is True.",
          "isDerived" : true,
          "isSlot" : false,
          "isView" : false,
          "name" : "isAtomic",
          "owner" : "_12345_0_be00301_1147682946132_981479_8",
          "type" : "Property"
        },
        "_12345_5EAPbeta_be00301_1148651295316_167207_450" : { "boolean" : [ true, false, true ],
            "string" : ["hello", "goodbye"],
            "integer": [1, 2,3,4,5,6],
            "double": [0.0, 1.3],
            "documentation" : "",
            "isDerived" : false,
            "isSlot" : false,
            "isView" : false,
            "name" : "hideMetatype",
            "owner" : "_12345_5EAPbeta_be00301_1148632817486_155219_3",
            "type" : "Property",
            "valueType" : "LiteralBoolean"
          },
    },
  "roots" : [ "_12345_5EAPbeta_be00301_1148301962580_941673_3150",
      "_12345_5EAPbeta_be00301_1147434586638_637562_1900",
      "_17_0_2_136f03d9_1344498413266_378771_11852"
    ]
}