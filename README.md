# OBDA Benchmarking Tools

These are tools developed by NB. Some of these tools share functionality with obda-converter by JE. 
The argument style generally follows those set out by RDFox 

```
java -jar <tool> -file1 <file location 1> -file2 <file location 2> ...
```

And so on from this. Running just ``` java -jar <tool> ``` will return the inputs needed. Generally an output file is one of these arguments but sometimes these are not required and the output will be printed to the command line. This functionality will be explained in the relevant sections.  



## Schema Generator

This takes in the st-tgds and t-tgds rules and returns source and target schemas. This is entirely uses the CB tools to this. With no file output location this will not print to the terminal and the results will be ignored so make sure they are included. This won't print anything to the command line

#### Arguments

    -st-tgds      <file>   | the file containing the source-to-target TGDs
    -t-tgds       <file>   | the file containing the target-to-target TGDs
    -s-schema     <file>   | the output file of the source schema
    -t-schema     <file>   | the output file of the target schema



## Ontop Mapping Generator

This takes in an st-tgd file in CB format and creates an ontop obda mapping file. Theses mapping files are a series of unary and binary mappings in the format of

```
mappingId <Mapping ID>
target  ns:ns/{var0} ns:<property> ns:ns/{var1} .
source  <SQL Statement>
```

for unary atoms or classes

```
mappingId <Mapping ID>
target  ns:<class>/{var0} a ns:<class> .
source  <SQL Statement>
```

for binary atoms or properties

These are created by going through the rules and filtering all the binary and unary target atoms then building these mapping from the rules. The sql statement is generated from the rule using the SQL converter that will be mentioned later. This currently will work with GAV (global as view or many to 1 rules) but I'm not sure about the correctness of LAV (1 to many rules). There are a bunch of owl related methods that are not used anymore but have been left if you wanted to expand the program. They mostly filter out only relevant atoms.

#### Arguments

    -st-tgds    <file>  | the file containing the source-to-target TGDs
    -out        <file>  | the file containing the query

This tool without the -out argument will print the output to the command line.

## Owl Generator

This converter is not complete. The output isn't formatted correctly but it does parse with all the owl parsers the tools use. It takes only a t-tgd file and returns an owl file. It also makes the "src_" linear tgds if you need those. And can be done at the same time as generating the owl file. There are a few known issues that are listed below

#### Arguments

    -t-tgds    <file>  | the file containing the target-to-target TGDs
    -out       <file>  | the file to print out the owl ontology
    -stSrc     <file>  | print a file containing 1-1 st-tgds

The output file location is required for this to work. Using the -stSrc will also create that fine in conjunction with the owl file, not instead of.

### Issues

There are a bunch of edge cases that will successfully create owl files that will fail to parse. A lot of this has to do with the sub-properties, domain and ranges not being explicit and other fun quirks. The converter has a classic case of being a bodge. The proper way of doing this would have required learning the OWLAPI and creating a way to recognise the t-tgd pattern for the different owl tags but that was taking too long.

- There are some implied domain and range constraints when they appear with explicit domain and range constraints there is an error. Maybe make a hierarchy of what is outputted?
- The use of inverseOf and a number of inverse properties that are used temporary fixes may sometimes interact with properties which are actually inverses.  

## Modify ChaseBench

This takes the old chasebench scenarios and makes them work with ontology based tools. As in it chops the atoms or relations so that they are only binary and unary. This sometimes will give problems with existential and meaningless unanswerable queries but they do usually run.

#### Arguments

    -st-tgds       <file>   | the file containing the source-to-target TGDs
    -t-tgds        <file>   | the file containing the target-to-target TGDs
    -q             <folder> | the folder containing queries in chasebench format
    
This tool overwrites the file locations it reads in. So make sure to make a copy before running this command. 

## Datalog To UCQ

This is used for getting the Tree Witness Rewriter (I also call it the ontop rewriter in places) to be compatible with the SQL converter mentioned in the next section. This converts the datalog output into a list of sub queries and a main query. It then will recursively substitute theses sub queries into the main query and all of those will be collected when no more substitutions can be made. This function also takes the datalog output as a cli argument that can be done using bash, not a file. Probably would be a nice feature to have if you wanted to do it. Wouldn't take very long.

This reads in strings from the command line and prints the output to the command line back. So no files are read for this. 

#### Arguments

    -exts       <string>  | the string contains the EXT rewritings
    -query      <string>  | the string containing the query


## SQL Converter

This is not really my code its a quite heavily modified version of Dario's (Not sure what his last name is) sql converter. It is being used as the one James wrote in JS is a tad buggy when getting the right column from a query string. While his parses directly from the output formats of Rapid, Graal and Iqaros. This is only in CB format and to get those tool to work with it requires some sed functions that are in query.sh. This has an src_ parameter which in the cli program just looks for another argument which added src_ to all table names so that it works with with the format of the 1-to-1 st-tgds. To fix the issue in ST Chase another parameter is added that will filter existential variables in the output using ```NOT LIKE '_:%'``` at the end of the query. Also this now has aliases added to it because there is sometimes a character limit to arguements in the script and that pushes the limit further away.

#### Arguments

    java -jar sqlConv.jar <ChaseBench query as a string> <printSrc>
    
   This is unique in the fact there are no options to this converter. It takes in a list of query as a string and prints out the sql query to the command line. Also the second argument for printing "src_" is just a length check of > 1 so it can be anything really. 

## ST Chase

Probably the most interesting tool in this project. It performs a single chase step on the query and is used in conjunction with BCA is greater scope. I've attempted to optimise this for parallelism and speed in the easiest to understand way, parallel streams. So most of the data structures are concurrent safe, like the atomic int counter. There is a large number of removing unnecessary information. Like removing rules not associated with the query and even atoms that are not relevant. The two slow parts of the tool is the substitution of data into the rules and then the insertion of these rules in a database (postgres). In theory a faster way might be possible by taking shortcuts in answering the query by avoiding the database. You already iterate through the substituted atoms so maybe checking for equivalence in memory would be faster than the expensive inserts. I'm pretty sure the substitution functions is as fast as it can be without an overhaul of the function. 

#### Arguments

    -s-sch      <file>   | the file containing the source schema in CB format
    -t-sch      <file>   | the file containing the target schema in CB format
    -t-sql      <file>   | the file containing the target schema in SQL format
    -q          <file>   | the file containing the query
    -st-tgds    <file>   | the file containing the source-to-target TGDs
    -data     <folder>   | the folder containing the csv files
    
This prints out extra information about the time to perform certain actions if that is something you want to add to the benchmarking tools.

### Issues 

- There is a small bug where the number of tuples is too small sometimes but I'm not sure where that race condition is. I think it is in insertions.

## Deep Data

This tool creates some basic data that was used in the deep scenarios. It will work generally and probably should be called "basic data".  Generally reads in the st-tgds and creates csv file containing "X0" for unary atoms or "X0,X1" for binary atoms. This set of data is significantly more likely to return tuples than the data generator from James at much smaller numbers  

## Graph Drawing

This is the only tool which is a python script and leverages the pyplot library. It takes in the folder location of the data size and returns a single graph and puts it in that folder. If you change the source it is possible to make the graph interactive like a pyplot graph but I've been using it for quick graphs. An example is ```python csvToGraph.py experiments/StockExchange/small``` or more formally.

```
python csvToGraph.py <folder location>
```
