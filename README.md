# Common Code Segment Selector

**Common** **code** **segment** **selector** \(C2S2\) is a independent tool to select common code segments for exclusion from code similarity detection. It accepts a set of programs and lists the common segments. The segments are subject to manual investigation before being excluded from similarity detection. If the similarity detection does not accommodate such exclusion, but can deal with uncompilable code, C2S2 can remove the common segments from that set of programs.
Further details can be seen in the corresponding paper (updated later). Currently, the tool covers two programming languages: Java and Python. 

## C2S2 Modes 
### select
Given a set of programs, this mode will list the common code segments. 

*Quick command*: 
```
select <input_dirpath> <programming_language> <output_filepath> 
```  

*Complete command*: 
```
select <input_dirpath> <programming_language> <output_filepath> <additional_keywords_path> <inclusion_threshold> <min_ngram_length> <max_ngram_length> <boolean_setting_1> <boolean_setting_2> ... <boolean_setting_n>
```  

### remove
Given a set of programs and a list of common code segments, this mode will exclude the segments from the set of programs. 

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
value: a floating number between 0 to 1 (inclusive).

## Acknowledgments
This tool uses [ANTLR](https://www.antlr.org/) to tokenise given programs.
