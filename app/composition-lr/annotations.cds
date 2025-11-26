using CompositionService as service from '../../srv/service';

annotate service.Headers with @(
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : 'Title',
            Value : title,
        },
        {
            $Type : 'UI.DataField',
            Label : 'Description',
            Value : description,
        },
    ],
    UI.FieldGroup #GeneralInformation : {
        $Type : 'UI.FieldGroupType',
        Data : [
            {
                $Type : 'UI.DataField',
                Label : 'Title',
                Value : title,
            },
            {
                $Type : 'UI.DataField',
                Label : 'Description',
                Value : description,
            },
        ],
    },
    UI.Facets : [
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'HeaderDetails',
            Label : 'Header Details',
            Target : '@UI.FieldGroup#GeneralInformation',
        },
        {
            $Type : 'UI.ReferenceFacet',
            ID : 'ItemsFacet',
            Label : 'Items',
            Target : 'items/@UI.LineItem',
        },
    ],
);

annotate service.Items with @(
    UI.LineItem : [
        {
            $Type : 'UI.DataField',
            Label : 'Item No',
            Value : itemNo,
        },
        {
            $Type : 'UI.DataField',
            Label : 'Product',
            Value : productName,
        },
        {
            $Type : 'UI.DataField',
            Label : 'Quantity',
            Value : quantity,
        },
        {
            $Type : 'UI.DataField',
            Label : 'Price',
            Value : price,
        },
        {
            $Type : 'UI.DataField',
            Label : 'Deleted',
            Value : isDeleted,
        },
    ],
);
