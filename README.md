# Common Code Segment Selector

**Common** **code** **segment** **selector** \(C2S2\) is a independent tool to select common code segments for exclusion from code similarity detection. It accepts a set of programs and lists the common segments. The segments are subject to manual investigation before being excluded from similarity detection. If the similarity detection does not accommodate such exclusion, but can deal with uncompilable code, C2S2 can remove the common segments from that set of programs.
Further details can be seen in the corresponding paper (updated later). Currently, the tool covers two programming languages: Java and Python. 

## C2S2 Modes 
### select
Given a set of programs, this mode will list the common segments. 
*Quick command*: 
```
select <input_dirpath> <programming_language> <output_filepath> 
```  

*Complete command*: 
```
select <input_dirpath> <programming_language> <output_filepath> <additional_keywords_path> <inclusion_threshold> <min_ngram_length> <max_ngram_length> <boolean_setting_1> <boolean_setting_2> ... <boolean_setting_n>
```  

## Acknowledgments
This tool uses [ANTLR](https://www.antlr.org/) to tokenise given programs.
