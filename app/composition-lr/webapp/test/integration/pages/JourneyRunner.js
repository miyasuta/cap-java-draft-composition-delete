sap.ui.define([
    "sap/fe/test/JourneyRunner",
	"customer/capjavacompositiondeletetest/compositionlr/test/integration/pages/ItemsList",
	"customer/capjavacompositiondeletetest/compositionlr/test/integration/pages/ItemsObjectPage"
], function (JourneyRunner, ItemsList, ItemsObjectPage) {
    'use strict';

    var runner = new JourneyRunner({
        launchUrl: sap.ui.require.toUrl('customer/capjavacompositiondeletetest/compositionlr') + '/test/flpSandbox.html#customercapjavacompositiondele-tile',
        pages: {
			onTheItemsList: ItemsList,
			onTheItemsObjectPage: ItemsObjectPage
        },
        async: true
    });

    return runner;
});

