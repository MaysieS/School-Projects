//
//  main.cpp
//  P5
//
//  Created by sunmeixi on 6/16/17.
//  Copyright © 2017 MeixiSun. All rights reserved.
//

#include <iostream>
#include <sstream>
#include <fstream>
#include <map>
#include <set>
#include <cmath>
#include <string>
#include "csvstream.h"
#include <iomanip>

using std::map;
using std::log;
using std::set;

using namespace std;

static set<string> unique_words(const string &str);

class Classifier {
public:
    
    int get_num_words() const {
        return num_words;
    }
    
    int get_num_posts() const {
        return num_posts;
    }
    
    void Initialize (csvstream &train) {
        csvstream::row_type row;
        csvstream::row_type junk;
        string label;
        string post;
        string line;
        string trash;
        //getline(train, trash);
        //train >> junk;
        
        while (train >> row) {
            int count = 0;
            for (auto col:row) {
                if (count == 1) {
                    label = col.second;
                }
                else {
                    post = col.second;
                }
                
                count++;
            }
            labels[label]++;
            label_posts.push_back({label, unique_words(post)});
            num_posts++;
        }
        
        /*while (getline(train, line)) {
            replace(line.begin(), line.end(), ',', ' ');
            stringstream ss(line);
            ss >> label;
            getline(ss, post);
            labels[label]++;
            label_posts.push_back({label, unique_words(post)});
            num_posts++;
        }*/
    }
    
    void Initialize_words() {
        for (auto i : label_posts) {
            for (auto j : i.second) {
                words.insert(j);
            }
        }
        num_words = (int)words.size();
    }
    
    void Initialize_label_words() {
        set<string> words;
        for (auto i: labels) {
            for (auto j: label_posts) {
                if (j.first == i.first) {
                    for (auto k: j.second) {
                        words.insert(k);
                    }
                }
            }
            label_words.insert({i.first, words});
        }
    }
    
    void Initialize_count_label_w() {
        for (auto i : label_words) {
            for (auto j : i.second) {
                int count = 0;
                for (auto k : label_posts) {
                    if (k.first == i.first) {
                        for (auto m : k.second) {
                            if (m == j) {
                                count++;
                            }
                        }
                    }
                }
                count_label_w.push_back({{i.first, j}, count});
            }
        }
    }
    
    int posts_contain_w (string w) {
        int count = 0;
        for (auto i : label_posts) {
            
            for (auto j : i.second) {
                if (j == w) {
                    count++;
                }
            }
        }
        return count;
    }
    
    int posts_labelc_contain_w(string label, string w) {
        int count = 0;
        
        for (auto i : label_posts) {
            if (i.first == label) {
                for (auto j : i.second) {
                    if (j == w) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
    
    int get_num_labels() const {
        return (int)labels.size();
    }
    
    map<string, int> & get_labels() {
        return labels;
    }
    
    int num_labels(string label) {
        for (auto i : labels) {
            if (i.first == label) {
                return i.second;
            }
        }
        return -1;
    }
    
    double calc_log_prior(string label) {
        double prior = 0;
        prior = log((double)num_labels(label) / num_posts);
        log_prior.insert({label, prior});
        return prior;
        
    }
    
    double calc_log_likelihood(string label, string w) {
        double likelihood = 0;
        bool in_file = false;
        bool with_label = false;
        for (auto i: words) {
            if (i == w) {
                in_file = true;
            }
        }
        if (in_file) {
            for (auto i: label_posts) {
                if (i.first == label) {
                    with_label = true;
                }
            }
        }
        if (with_label) {
            likelihood = log((double)posts_labelc_contain_w(label, w) / num_labels(label));
        }
        else if (in_file) {
            likelihood = log((double)posts_contain_w(w) / num_posts);
        }
        else {
            likelihood = log((double)1 / num_posts);
        }
        log_likelihood.insert({{label, w}, likelihood});
        return likelihood;
    }
    
    void calc_log_prob_score(string post) {
        for (auto i : labels) {
            istringstream iss(post);
            string word;
            double score = calc_log_prior(i.first);
            while (iss >> word) {
                score += calc_log_likelihood(i.first, word);
            }
            log_probability.insert({i.first, score});
        }
    }
    
    pair<string,double> find_max_prob_score() {
        double max = (*log_probability.begin()).second;
        string max_label = (*log_probability.begin()).first;
        for (auto i : log_probability) {
            if (i.second > max) {
                max = i.second;
                max_label = i.first;
            }
        }
        log_probability.clear();
        return {max_label, max};
    }
    
    void clear_log_probability() {
        log_probability.clear();
    }
    
    void print_label_posts(csvstream &train) {
        csvstream::row_type row;
        csvstream::row_type junk;
        string line;
        string label;
        string post;
        string trash;
        //train >> junk;
        //getline(train, trash);
        
        cout << "training data:" << endl;
        while (train >> row) {
            int count = 0;
            for (auto col:row) {
                if (count == 1) {
                    label = col.second;
                }
                else {
                    post = col.second;
                }

                
                count++;
            }
            cout << "  label = " << label << ", content = " << post << endl;
        }
        /*while (getline(train, line)) {
            replace(line.begin(), line.end(), ',', ' ');
            stringstream ss(line);
            ss >> label;
            getline(ss, post);
            cout << "  label = " << label << ", content = " << post << endl;
        }*/
    }
    
    void print_num_posts() {
        cout << "trained on " << num_posts << " examples" << endl;
    }
    
    void print_num_words() {
        cout << "vocabulary size = " << num_words << endl;
    }
    
    void print_classes() {
        cout << "classes:" << endl;
        for (auto i : labels) {
            cout << "  " << i.first << ", " << i.second << " examples, log-prior = " << calc_log_prior(i.first) << endl;
        }
    }
    
    void print_label_words() {
        cout << "classifier parameters:" << endl;
        for (auto i: count_label_w) {
            cout << "  " << i.first.first << ":" << i.first.second << ", count = " << i.second << ", log-likelihood = " << calc_log_likelihood(i.first.first, i.first.second) << endl;
        }
    }
    

    
private:
    // total number of posts in the training file
    int num_posts;
    // total number of unique words in the training file
    int num_words;
    // a set of strings containing all the unique words
    set<string> words;
    // a map of labels with their times of appearance
    map<string, int> labels;
    // a vector of posts (unique words) paired with labels
    vector<pair<string, set<string>>> label_posts;
    //a map of label and its log prior
    map<string, double> log_prior;
    // a map of pairing of labels and words with the log likelihood
    map<pair<string, string>, double> log_likelihood;
    //a map of label and its log probability
    map<string, double> log_probability;
    // a map of labels with all unique words with it
    map<string, set<string>> label_words;
    // a map of labels with the count of each word comes with the label
    vector<pair<pair<string, string>, int>> count_label_w;
};

int main(int argc, const char * argv[]) {
    cout << setprecision(3);
    if (! (argc == 3 || argc ==4)) {
        cout << "Usage: main TRAIN_FILE TEST_FILE [--debug]" << endl;
        return -1;
    }
    
    bool debug = false;
    if (argc == 4) {
        if (strcmp(argv[3], "--debug") != 0) {
            cout << "Usage: main TRAIN_FILE TEST_FILE [--debug]" << endl;
            return -1;
        }
        else {
            debug = true;
        }
    }
    
    string train_file = argv[1];
    string test_file = argv[2];
    csvstream train(train_file);
    csvstream train1(train_file);
    csvstream test(test_file);
    //ifstream train, test;
    //train.open(train_file);
    //train1.open("train_small.csv");
    //test.open(test_file);
    csvstream_exception ce("Error opening file: " + train_file + '\n');
    /*if (!train.is_open()) {
        cout << "Error opening file: " << train_file << endl;
        return -1;
    }
    if (!train1.is_open()) {
        cout << "Error opening file: " << train_file << endl;
        return -1;
    }
    if (!test.is_open()) {
        cout << "Error opening file: " << test_file << endl;
        return -1;
    }*/
    
    Classifier c;
    c.Initialize(train);
    c.Initialize_words();
    c.Initialize_label_words();
    c.Initialize_count_label_w();
    if (debug) {
        c.print_label_posts(train1);
    }
    
    c.print_num_posts();
    if (debug) {
        c.print_num_words();
    }
    cout << endl;
    if (debug) {
        c.print_classes();
        c.print_label_words();
        cout << endl;
    }
    
    cout << "test data:" << endl;
    int count = 0;
    int num_post = 0;
    csvstream::row_type row;
    csvstream::row_type junk;
    string line;
    string label;
    string post;
    string trash;
    //getline(test, trash);
    //test >> junk;
    while (test >> row) {
        int count1 = 0;
        for (auto col:row) {
            if (count1 == 1) {
                label = col.second;
            }
            else {
                post = col.second;
            }
            count1++;
        }
        num_post++;
        c.calc_log_prob_score(post);
        pair<string, double> result = c.find_max_prob_score();
        cout << "  correct = " << label << ", predicted = " << result.first << ", log-probability score = " << result.second << endl;
        cout << "  content = " << post << endl << endl;
        if (label == result.first) {
            count++;
        }
        c.clear_log_probability();
    }
    cout << "performance: " << count << " / " << num_post << " posts predicted correctly" << endl;
    
    return 0;
}

static set<string> unique_words(const string &str) {
    // Fancy modern C++ and STL way to do it
    istringstream source{str};
    return {istream_iterator<string>{source},
        istream_iterator<string>{}};
}
