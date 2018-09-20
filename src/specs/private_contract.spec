# Private Contract

This is to verify that a private contract between 2 parties are not accessible by others

* Private contract is established between "Node1" and "Node7".

## Contract is mined
tags: private,mining

* Transaction Hash is returned.
* Transaction Receipt is present in "Node1".
* Transaction Receipt is present in "Node7".

## Privacy is enforced between participated nodes
tags: privacy

* "Node1" and "Node7" see the same value.
* "Node3" must not be able to see the same value.

## When there's an update, privacy is still enforced
tags: privacy

* "Node1" updates to new value "4" with "Node7".
* "Node1" and "Node7" see the same value.
* "Node3" must not be able to see the same value.