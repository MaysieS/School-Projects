//
//  main.cpp
//  Project4
//
//  Created by sunmeixi on 6/5/17.
//  Copyright © 2017 MeixiSun. All rights reserved.
//

#include <iostream>
#include <string>
#include"Stack.h"

using namespace std;

int main(int argc, const char * argv[]) {
    cout.precision(4);
    
    Stack<double> stack;
    
    string input = "";
    
    while (cin >> input && input != "q") {
        if (input == "+") {
            assert(stack.size() >= 2);
            double expr1 = stack.pop();
            double expr2 = stack.pop();
            double sum = expr1 + expr2;
            stack.push(sum);
        }
        else if (input == "-") {
            assert(stack.size() >= 2);
            double expr1 = stack.pop();
            double expr2 = stack.pop();
            double subtraction = expr2 - expr1;
            stack.push(subtraction);
        }
        else if (input == "*") {
            assert(stack.size() >= 2);
            double expr1 = stack.pop();
            double expr2 = stack.pop();
            double product = expr1 * expr2;
            stack.push(product);
        }
        else if (input == "/") {
            assert(stack.size() >= 2);
            double expr1 = stack.pop();
            double expr2 = stack.pop();
            if (expr1 == 0) {
                cout << "Error: Division by zero" << endl;
            }
            else {
                double result = (double)expr2 / expr1;
                stack.push(result);
            }
        }
        else if (input == "d") {
            assert(stack.size() >= 1);
            double expr = stack.pop();
            stack.push(expr);
            stack.push(expr);
        }
        else if (input == "r") {
            assert(stack.size() >= 2);
            double expr1 = stack.pop();
            double expr2 = stack.pop();
            stack.push(expr1);
            stack.push(expr2);
        }
        else if (input == "p") {
            assert(stack.size() >= 1);
            cout << stack.top() << endl;
        }
        else if (input == "c") {
            while (!stack.empty()) {
                stack.pop();
            }
        }
        else if (input == "a") {
            cout << stack << endl;
        }
        else if (input == "n") {
            assert(stack.size() >= 1);
            double expr = stack.pop();
            expr = (-1) * expr;
            stack.push(expr);
        }
        else {
            string::size_type st;
            double expr = stod(input, &st);
            stack.push(expr);
        }
    }
    
    return 0;
    
}
