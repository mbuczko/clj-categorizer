# Category tree manager

This is a rough attempt to create versatile category manager which would be simple to understand and powerful enough to handle different scenarios.
What are all those categories and where the problem actually arises?

Categories are everywhere. We use them to classify things, enclose same features into named groups which usually have their own subgroups which have their own subgroups and so on.
One of the real world examples are cars. We split them by function they were built for (eg. trucks, agriculturals...), by make (BMW, VW), by model or serie.
Soon it's quite obvious that all categories together can be easily visualized as a tree. In case of car we may imagine categories as a tree with following levels:

    car -> make -> serie -> model

for example:

    car -> BMW -> Serie X -> X30

Alright, but having categories with no properties assigned is simply useless. Each category has its own set of properties which in case of cars decide why we prefer Tarpan over BMW ;)
Properies may be exactly the same for several categories (eg. most of cars have an ABS today), may be unique for one category or be excluded from the other. This is where the scenarios mentioned earlier come onto scene.

If we try to assign properies in the most naive may - one by one for each category separately, it may succeed for a small amount of categories. It will fail painfully for cars (with roughly 5k of makes and models).

Let's visualize it as a _Scenario 1_:

![scenario1](https://github.com/mbuczko/categorizer/blob/master/scenario1.png "scenario 1")

We have a simplified tree of cars with 3 makes and BMW X30 is a sneaky one having ABS in a standard. Assigning ```:has-abs``` property was trivial, but one day your Pointy Haired Boss announces:

> _Guys, we have a problem. Acura, Tarpan and all 3000 of makes we store have ABS-es as well!_

Good luck with trying to assign properties by hand. This is where we switch to _Scenario 2_:

![scenario2](https://github.com/mbuczko/categorizer/blob/master/scenario2.png "scenario 2")

The idea behind is simple - if all our cars have ABS-es, instead of crazy assigning ```:has-abs``` separately for each make and models, let's assign it once for _car_ node and mark it as _inherited_, which would make it assigned for all the nodes below by default.
In other words all the makes and models will _inherit_ property from their parent node and do not need explicit assignment.

That's how our fantasy world look like. In fact Tarpans have no ABS-es (but hey, they still have some other nice stuff inside!). Surely, they're exception but it doesn't mean that we have to switch back to _Scenario 1_. Instead let's examine _Scenario 3_:

![scenario3](https://github.com/mbuczko/categorizer/blob/master/scenario3.png "scenario 3")

Yup, simple like this. We still keep inherited ```:has-abs``` at top but additionally we marked Tarpans as an exception which should have ```:has-abs``` excluded. This way we avoid _Scenario 1_ with separate assignments still having nice way to exclude property from certain nodes.

Now, let's imagine for a moment that BMW is the make with no ABS-es under the hood (looking at some BMW drivers it's not as hard to imagine that). BMW has lot of series and models which would make us exclude property multiple times (if we followed _Scenario 3_). Fortunatell, it could be simplfied again. Let's welcome _Scenario 4_ - the last one.

![scenario4](https://github.com/mbuczko/categorizer/blob/master/scenario4.png "scenario 4")

That's right - we're using inheritance again, this time to make exclusion easier. Instead of assigning exluded ```:has-abs```  to each BMW subnode, we may do it once (at BMW level) and mark exclusion as inherited. That means, all the subnodes have no ABS-es by default with no explicit exclusions made before.
#Example

[todo]

##LICENSE

Copyright © Michał Buczko

Licensed under the EPL.
