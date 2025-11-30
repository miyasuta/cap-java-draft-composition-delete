using { com.example as my } from '../db/schema';

service CompositionService @(path: '/odata/v4/composition') {
  @odata.draft.enabled
  entity Headers as projection on my.Headers;

  entity Items as projection on my.Items;
}

annotate CompositionService.Items with {
  isDeleted @readonly;
}