namespace com.example;

using { cuid, managed } from '@sap/cds/common';

entity Headers : cuid, managed {
  title       : String(100);
  description : String(500);
  items       : Composition of many Items on items.header = $self;
}

entity Items : cuid, managed {
  header      : Association to Headers;
  itemNo      : Integer;
  productName : String(100);
  quantity    : Integer;
  price       : Decimal(10, 2);
}
