# Information Retrieval Toolbox

## Project Overview

This comprehensive Information Retrieval Toolbox was developed as part of COMPSCI 446, focusing on search engines and Information Retrieval (IR). It encompasses five distinct modules, each tailored to enhance specific aspects of the information retrieval process.

## Modules

### 1. Setup Module

**Description:**
This module initializes the project and processes a gzipped input file, capturing essential statistics.

**Functionality:**
- Reads and processes lines from a gzipped input file.
- Writes the k-th token to the output file for lines with sufficient tokens.
- Tracks and outputs total lines processed, along with min and max values.

**Output Files:**
- `output_setup.txt`

### 2. Tokenization Module

**Description:**
A Java program performing advanced text tokenization on a gzipped input file.

**Functionality:**
- Implements various tokenization methods, stopword removal, and stemming algorithms.
- Handles URLs, numbers, abbreviations, hyphenated words, and punctuation.
- Calculates word frequencies, unique tokens, and the top 100 most frequent words.
- Implements the Porter stemming algorithm and handles short words.

**Output Files:**
- `output_tokens.txt`: Tokenization results.
- `output_statistics.txt`: Tokenization statistics.
- `output_frequent_words.txt`: Top 100 most frequent words.

### 3. Query Processing Module

**Description:**
Evaluates the performance of an information retrieval system, computing various metrics.

**Metrics:**
- NDCG@20
- numRel
- RR (Reciprocal Rank)
- P@10 (Precision at 10)
- R@10 (Recall at 10)
- F1@10 (F1 Score at 10)
- P@20% (Precision at 20% recall)
- MAP (Mean Average Precision)

**Output Files:**
- `output_query_metrics.txt`

### 4. Posting Module

**Description:**
Implements a simple information retrieval system, building an index and processing queries.

**Query Types:**
1. "or": Union of query terms.
2. "and": Intersection of query terms.
3. "ql": Query Likelihood model.

**Input Files:**
- Queries: `queries.tsv`
- Collection: `collection.json.gz`

**Output Files:**
- `output_retrieval_results.txt`

### 5. Pagerank Module

**Description:**
Calculates PageRank values for a web graph.

**Parameters:**
- Input file, lambda, tau, inlinks filename, pagerank filename, and top k results.

**Output Files:**
- `output_pagerank.txt`: Top k pages based on PageRank values.
- `output_inlinks.txt`: Top k pages based on inlinks.
