# Search Engines and Information Retrieval

## Overview

This comprehensive project for CMPSCI 446 explores various modules focusing on search engines, information retrieval, and web graph analysis. The project covers a range of topics outlined in the course description, offering practical implementations and tools for different aspects of information retrieval.

## Table of Contents

1. [Setup Module](#setup-module)
   - [Description](#setup-description)
   - [Functionality](#setup-functionality)
   - [Output](#setup-output)

2. [Tokenization Module](#tokenization-module)
   - [Description](#tokenization-description)
   - [Functionality](#tokenization-functionality)
   - [Statistics](#tokenization-statistics)

3. [Query Processing Module](#query-processing-module)
   - [Description](#query-processing-description)
   - [Metrics](#query-processing-metrics)

4. [Posting Module](#posting-module)
   - [Description](#posting-description)
   - [Query Types](#posting-query-types)

5. [Pagerank Module](#pagerank-module)
   - [Description](#pagerank-description)
   - [Parameters](#pagerank-parameters)
   - [Output](#pagerank-output)

---

## 1. Setup Module<a name="setup-module"></a>

### Description<a name="setup-description"></a>

The **Setup Module** initializes the project by processing a gzipped input file. It performs basic validation and captures essential statistics about the dataset.

### Functionality<a name="setup-functionality"></a>

- Reads each line from the gzipped input file.
- Splits each line into tokens using space as a delimiter.
- Writes the k-th token to the output file if the line has sufficient tokens.
- Tracks the minimum and maximum values encountered.
- Writes total lines processed, along with min and max values, to the output file.

### Output<a name="setup-output"></a>

- Output File: `output_setup.txt`
- Contents:
  - "Too Short" for lines with insufficient tokens.
  - The k-th token for valid lines.
  - Total lines processed.
  - Minimum and maximum values.

## 2. Tokenization Module<a name="tokenization-module"></a>

### Description<a name="tokenization-description"></a>

The **Tokenization Module** is a Java program performing advanced text tokenization on a gzipped input file. It covers multiple tokenization methods, stopword removal, stemming algorithms, and provides detailed statistics.

### Functionality<a name="tokenization-functionality"></a>

- Tokenization methods, stopword removal, and stemming.
- Handling URLs, numbers, abbreviations, hyphenated words, and punctuation.
- Calculating word frequencies, unique tokens, and top 100 most frequent words.
- Implementing the Porter stemming algorithm and handling short words.

### Statistics<a name="tokenization-statistics"></a>

- Output Files:
  - `output_tokens.txt`: Tokenization results.
  - `output_statistics.txt`: Tokenization statistics.
  - `output_frequent_words.txt`: Top 100 most frequent words.

## 3. Query Processing Module<a name="query-processing-module"></a>

### Description<a name="query-processing-description"></a>

The **Query Processing Module** defines a class for evaluating the performance of an information retrieval system. It computes various metrics by comparing the system's output to relevance judgments.

### Metrics<a name="query-processing-metrics"></a>

- NDCG@20
- numRel
- RR (Reciprocal Rank)
- P@10 (Precision at 10)
- R@10 (Recall at 10)
- F1@10 (F1 Score at 10)
- P@20% (Precision at 20% recall)
- MAP (Mean Average Precision)

## 4. Posting Module<a name="posting-module"></a>

### Description<a name="posting-description"></a>

The **Posting Module** implements a simple information retrieval system. It builds an index from a JSON file containing documents and processes queries to retrieve relevant documents.

### Query Types<a name="posting-query-types"></a>

1. "or": Union of query terms.
2. "and": Intersection of query terms.
3. "ql": Query Likelihood model.

## 5. Pagerank Module<a name="pagerank-module"></a>

### Description<a name="pagerank-description"></a>

The **Pagerank Module** calculates PageRank values for a web graph. It takes command-line arguments and provides an efficient implementation of the PageRank algorithm.

### Parameters<a name="pagerank-parameters"></a>

- Input file, lambda, tau, inlinks filename, pagerank filename, and top k results.

### Output<a name="pagerank-output"></a>

- Output Files:
  - `output_pagerank.txt`: Top k pages based on PageRank values.
  - `output_inlinks.txt`: Top k pages based on inlinks.

---
