# Privacy Marker Transaction

 Tags: privacy-precompile-enabled

## Private transactions create Privacy Marker Transactions

 Tags: private

* Deploy a simple smart contract with initial value "42" in "Node1"'s default account and it's private for "Node4", named this contract as "contract17"
* Transaction Hash is returned for "contract17"
* "contract17"'s creation transaction retrieved by "Node1" is a privacy marker transaction
* Execute "contract17"'s `set()` function with new value "5" in "Node1" and it's private for "Node4", store transaction hash as "contract17.set5"
* Transaction "contract17.set5" retrieved by "Node1" is a privacy marker transaction
