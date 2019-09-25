# OBDA Benchmarking Tools

These are tools developed by NB. Some of these overlap with obda-converter by JE

## Schema Generator

This takes in the st-tgds and t-tgds rules and returns source and target schemas. This is entirely uses the CB tools to this.

## Ontop Mapping Generator

This takes in an st-tgd file in CB format and creates an ontop obda mapping file. Theses mapping files are a series of unary and binary mappings in the format of

```
mappingId <Mapping ID>
target  ns:ns/{var0} ns:<property> ns:ns/{var1} .
source  <SQL Statement>"
```

for unary atoms or classes

```
mappingId <Mapping ID>
target  ns:<class>/{var0} a ns:<class> .
source  <SQL Statement>"
```

for binary atoms or properties

These are created by going through the rules and filtering all the binary and unary target atoms then building these mapping from the rules. The sql statement is generated from the rule using the SQL converter that will be mentioned later. This currently will work with GAV (global as view or many to 1 rules) but I'm not sure about the correctness of LAV (1 to many rules). There are a bunch of owl related methods that are not used anymore but have been left if you wanted to expand the program. They mostly filter out only relevant atoms.

## Owl Generator

This converter is not complete. There are a bunch of edge cases that will successfully create owl files that will fail to parse. A lot of this has to do with the sub-properties, domain and ranges not being explicit and other fun quirks. The converter has a classic case of being a bodge. The proper way of doing this would have required learning the OWLAPI and creating a way to recognise the t-tgd pattern for the different owl tags but that was taking too long. Thats why the output looks wrong but it does parse with all the owl parsers the tools use. It takes only a t-tgd file and returns an owl file. It also makes the "src_" linear tgds if you need those. And can be done at the same time as generating the owl file.

## Modify ChaseBench

This takes the old chasebench scenarios and makes them work with ontology based tools. As in it chops the atoms or relations so that they are only binary and unary. This sometimes will give problems with existential and meaningless unanswerable queries but they do usually run.

## Datalog To UCQ

This is used for getting the Tree Witness Rewriter (I also call it the ontop rewriter in places) to be compatible with the SQL converter mentioned in the next section. This converts the datalog output into a list of sub queries and a main query. It then will recursively substitute theses sub queries into the main query and all of those will be collected when no more substitutions can be made. This function also takes the datalog output as a cli argument that can be done using bash, not a file. Probably would be a nice feature to have if you wanted to do it. Wouldn't take very long. 

## SQL Converter

This is not really my code its a quite heavily modified version of Dario's (Not sure what his last name is) sql converter. It is being used as the one James wrote in JS is a tad buggy when getting the right column from a query string. While his parses directly from the output formats of Rapid, Graal and Iqaros. This is only in CB format and to get those tool to work with it requires some sed functions that are in query.sh. This has an src_ parameter which in the cli program just looks for another argument which added src_ to all table names so that it works with with the format of the 1-to-1 st-tgds.

## Single Step Chase

Probably the most interesting tool in this project. It performs a single chase step on the query and is used in conjunction with BCA is greater scope. I've attempted to optimise this for parallelism and speed in the easiest to understand way, parallel streams. So most of the data structures are concurrent safe, like the atomic int counter. There is a large number of removing unnecessary information. Like removing rules not associated with the query and even atoms that are not relevant. The two slow parts of the tool is the substitution of data into the rules and then the insertion and querying of these rules in a database (postgres). In theory a faster way might be possible by taking shortcuts in answering the query but the substitution is as fast as I think it will go. There is a small bug where the number of tuples is too small but I'm not sure where that race condition is. I assume its probably in the substitution where two are trying to be inserted at the same time. Also there are existential variables in the final output that I need to remove but I'm not sure how to put the ```NOT LIKE '_:%'``` into the SQL statement in the correct place.

## Deep Data

This tool creates some basic data that was used in the deep scenarios. It will work generally and probably should be called "basic data".  Generally reads in the st-tgds and creates csv file containing "X0" for unary atoms or "X0,X1" for binary atoms. This set of data is significantly more likely to return tuples than the data generator from James at much smaller numbers  

## Graph Drawing

This is the only tool which is a python script and leverages the pyplot library.
