# CAP Java Composition Delete Test

This project demonstrates how to implement logical deletes for composition items in a CAP Java service. Draft-enabled headers with composed items are exposed via OData V4, and delete operations from Fiori List Report-style navigation paths must not physically remove the child records.

## Key Points

- `CompositionDeleteEventHandler` intercepts `DraftService.EVENT_DRAFT_CANCEL` on items and updates the `isDeleted` flag instead of cascading physical deletes. The handler inspects the incoming CQN so it works for both direct entity deletes and header->items navigation deletes.
- `CompositionDeleteTest` covers two scenarios: deleting items directly via their entity endpoint and deleting them through the header navigation path. Both tests assert that `isDeleted` remains `true` in draft and active states to guard against regressions.
