# Common Code Segment Selector

**Common** **code** **segment** **selector** \(C2S2\) is a independent tool to select common code segments for exclusion from code similarity detection. It accepts a set of student submissions and lists the common segments. The segments are subject to manual investigation before being excluded from similarity detection. If the similarity detection does not accommodate such exclusion, but can deal with uncompilable code, C2S2 can remove the common segments from that set of programs.
Further details can be seen in [the corresponding paper](https://dl.acm.org/doi/10.1145/3408877.3432436) published at 52nd ACM Technical Symposium on Computer Science Education (SIGCSE 2021). Currently, the tool covers two programming languages: Java and Python. 

## C2S2 Modes 
### select
This mode lists any common segments from given student submissions and stores the result in an output file.  

*Quick command*: 
```
select <input_dirpath> <programming_language> <output_filepath> 
```  

*Complete command*: 
```
select <input_dirpath> <programming_language> <output_filepath> <additional_keywords_path> <inclusion_threshold> <min_ngram_length> <max_ngram_length> coderesult generalised startident lineexclusive subremove
```  
 Any of the last five arguments can be removed to adjust the selection's behaviour. Further details about those can be seen below.


### remove
This mode removes common code segments from given student submissions. This accepts a directory containing the code files and generates the results under a new directory named '[result]' + given input directory.

*Command*: 
```
remove <input_dirpath> <programming_language> <common_code_filepath> <additional_keywords_path> <common_code_type>
```  

## Parameters description \(sorted alphabetically\):  
### <additional_keywords_path>
A string representing a file containing additional keywords with newline as the delimiter. Keywords with more than one token should be written by embedding spaces between the tokens. For example, 'System.out.print' should be written as \'System . out . print\'. If unused, please set this to \'null\'.  
### <common_code_filepath>
A string representing a file containing common code segments. The file can be either the mode 1's output or an arbitrary code written in compliance to the programming language's syntax.
### <common_code_type>
A string that should be either 'code', 'codegeneralised', or 'complete'. The first one means the common code file is a regular code file. The second one is similar to the first except that the code tokens will be generalised prior compared for exclusion. The third one means the common code file is the mode 1's output without 'coderesult' parameter.
### <input_dirpath>
A string representing the input directory containing student submissions (each submission is represented by either one file or one sub-directory). Please use quotes if the path contains spaces.
### <inclusion_threshold>
A floating number representing the minimum percentage threshold for common segment inclusion. Any segments which submission occurrence proportion is higher than or equal to the threshold are included. This is assigned with 0.75 by default; all segments that occur in more than or equal to three fourths of the submissions are included.  
Value: a floating number between 0 to 1 (inclusive).
### <max_ngram_length>
A number depicting the largest n-gram length of the filtered common segments. This is assigned 50 by default.  
Value: a positive integer higher than <min_ngram_length>.
### <min_ngram_length>
A number depicting the smallest n-gram length of the filtered common segments. This is assigned 10 by default.  
Value: a positive integer.
### <output_filepath>
A string representing the filepath of the output, containing the common segments. Please use quotes if the path contains spaces.
### <programming_language>
A constant depicting the programming language used on given student submissions.  
Value: 'java' (for Java) or 'py' (for Python).
### 'coderesult'
This ensures the suggested segments are displayed as raw code instead of generalised while having no information about the variation. The segments can be passed directly to a code similarity detection tool for exclusion. It is set true by default.
### 'generalised'
This enables token generalisation while selection common segments. It is set true by default. See the paper for details.
### 'lineexclusive'
This ensures the common segment selection only considers segments that start at the beginning of a line and end at the end of a line. It is set as true by default. See the paper for details.
### 'startident'
This ensures the common segment selection only considers segments that start with identifier or keyword. It is set true by default. See the paper for details.
### 'subremove'
This removes any common segments that are a part of longer fragments from the result. It is set true by default. See the paper for details.

## Acknowledgments
This tool uses [ANTLR](https://www.antlr.org/) to tokenise given programs. It also adapts [arunjeyapal's implementation of RKR-GST](https://github.com/arunjeyapal/GreedyStringTiling) to remove common code segments.
