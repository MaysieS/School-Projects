//
//  euchre.cpp
//  project03
//
//  Created by sunmeixi on 5/24/17.
//  Copyright © 2017 MeixiSun. All rights reserved.
//


#include "Card.h"
#include "Pack.h"
#include "Player.h"
#include <string>
#include <vector>
#include <cassert>
#include <algorithm>
#include <iostream>
#include <fstream>
#include <array>

using namespace std;

const int TRICK_CARD_SIZE = 4;


static int find_trick_max(Card trick_card[], const string &trump, const Card &led_card);


class Game {
    
public:
    
    
    void Initialize_pack(string file) {
        ifstream pack_in;
        pack_in.open(file);
        if (!pack_in.is_open()) {
            cout << "Error opening " << file << endl;
        }
        pack = Pack(pack_in);
    }
    void Initialize_table(string name1, string type1,  string name2, string type2, string name3, string type3, string name4, string type4) {
        Player *p0 = Player_factory(name1, type1);
        Player *p1 = Player_factory(name2, type2);
        Player *p2 = Player_factory(name3, type3);
        Player *p3 = Player_factory(name4, type4);
        
        table.push_back(p0);
        table.push_back(p1);
        table.push_back(p2);
        table.push_back(p3);

    }
    
    void Shuffle_pack (string shuffle_in) {
        if (shuffle_in == "shuffle") {
            pack.shuffle();
        }
        else {
            pack.reset();
        }
    }
    
    void Deal() {
        dealer_index = hand % 4;
        
        table[(dealer_index + 1) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 1) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 1) % 4]->add_card(pack.deal_one());
        
        table[(dealer_index + 2) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 2) % 4]->add_card(pack.deal_one());
        
        table[(dealer_index + 3) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 3) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 3) % 4]->add_card(pack.deal_one());
        
        table[(dealer_index + 4) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 4) % 4]->add_card(pack.deal_one());
        
        table[(dealer_index + 1) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 1) % 4]->add_card(pack.deal_one());
        
        table[(dealer_index + 2) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 2) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 2) % 4]->add_card(pack.deal_one());
        
        table[(dealer_index + 3) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 3) % 4]->add_card(pack.deal_one());
        
        table[(dealer_index + 4) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 4) % 4]->add_card(pack.deal_one());
        table[(dealer_index + 4) % 4]->add_card(pack.deal_one());
        
        upcard = pack.deal_one();
        cout << upcard << " turned up" << endl;

    }
    
    void Making_trump() {
        dealer_index = hand % 4;
        while (!trump_made) {
            for (int i = 1; i < 9; ++i) {
                if (table[(dealer_index + i) % 4]->make_trump(upcard, i %4 == 0, ((i - 1) / 4) + 1, trump)) {
                     cout << table[(dealer_index + i) % 4]->get_name() << " orders up " << trump << endl << endl;
                    trump_maker_index = dealer_index + i % 4;
                    trump_made = true;
                    if (i <= 4) {
                        table[dealer_index]->add_and_discard(upcard);
                    }
                    return;
                }
                else {
                    cout << table[(dealer_index + i) % 4]->get_name() << " passes" << endl;
                }
            }
        }

    }
    
    void Play() {
        dealer_index = hand % 4;
        leader_index = (dealer_index + 1) % 4;
        led_card = table[leader_index]->lead_card(trump);
        cout << led_card << " led by " << table[leader_index]->get_name() << endl;
        trick_card[leader_index] = led_card;
        
        for (int i = 1; i < 4; ++i) {
            trick_card[(leader_index + i) % 4] = table[(leader_index + i) % 4]->play_card(led_card, trump);
            cout << trick_card[(leader_index + i) % 4] << " played by " << table[(leader_index + i) % 4]->get_name() << endl;
        }
        if (find_trick_max(trick_card, trump, led_card) % 2 == 0) {
            winning_trick_02++;
        }
        else {
            winning_trick_13++;
        }
        leader_index = find_trick_max(trick_card, trump, led_card);
        cout << *table[leader_index] << " takes the trick" << endl << endl;
        
        for (int i = 1; i < trick; ++i) {
            led_card = table[leader_index]->lead_card(trump);
            cout << led_card << " led by " << table[leader_index]->get_name() << endl;
            trick_card[leader_index] = led_card;
            
            for (int i = 1; i < 4; ++i) {
                trick_card[(leader_index + i) % 4] = table[(leader_index + i) % 4]->play_card(led_card, trump);
                cout << trick_card[(leader_index + i) % 4] << " played by " << table[(leader_index + i) % 4]->get_name() << endl;
            }
            
            if (find_trick_max(trick_card, trump, led_card) % 2 == 0) {
                winning_trick_02++;
            }
            else {
                winning_trick_13++;
            }
            leader_index = find_trick_max(trick_card, trump, led_card);
            cout << *table[leader_index] << " takes the trick" << endl << endl;
        }
        
        if (winning_trick_02 >= 3) {
            cout << *table[0] << " and " << *table[2] << " win the hand" << endl;
            if (trump_maker_index % 2 == 0) {
                if (winning_trick_02 == 5) {
                    pt_02 += 2;
                    cout << "march!" << endl;
                }
                else {
                    pt_02++;
                }
            }
            else {
                pt_02+= 2;
                cout << "euchred!" << endl;
            }
        }
        else {
            cout << *table[1] << " and " << *table[3] << " win the hand" << endl;
            if (trump_maker_index % 2 == 1) {
                if (winning_trick_13 == 5) {
                    pt_13 += 2;
                    cout << "march!" << endl;
                }
                else {
                    pt_13 ++;
                }
            }
            else {
                pt_13 += 2;
                cout << "euchred!" << endl;
            }
        }
        hand++;
        winning_trick_13 = 0;
        winning_trick_02 = 0;
        trump_made = false;
        trump = "";
        cout << *table[0] << " and " << *table[2] << " have " << pt_02 << " points" << endl;
        cout << *table[1] << " and " << *table[3] << " have " << pt_13 << " points" << endl << endl;
    }

    
    int get_pt_02 () const {
        return pt_02;
    }
    
    int get_pt_13 () const {
        return pt_13;
    }
    
    int get_hand () const {
        return hand;
    }
    
    Player* get_dealer () {
        dealer_index = hand % 4;
        return table[dealer_index];
    }
    
    void Print_winning_message(int pt_to_win) {
        if (pt_02 >= pt_to_win) {
            cout << *table[0] << " and " << *table[2] << " win!" << endl;
        }
        else {
            cout << *table[1] << " and " << *table[3] << " win!" << endl;
            
        }
    }
    
    
private:

    vector<Player*> table;
    Card trick_card[TRICK_CARD_SIZE];
    Pack pack;
    int hand = 0, trick = 5;
    int dealer_index = 0, leader_index = 0, trump_maker_index = 0, trick_taker_index = 0;
    bool dealer = false;
    string trump = "";
    bool trump_made = false;
    Card upcard;
    Card led_card;
    int winning_trick_02 = 0, winning_trick_13 = 0;
    int pt_02 = 0, pt_13 = 0;
    
};


int main(int argc, char* argv[]) {
    
    Game game;
    
    int pt_to_win = atoi(argv[3]);
    game.Initialize_pack(argv[1]);
    
    game.Initialize_table(argv[4], argv[5], argv[6], argv[7], argv[8], argv[9], argv[10], argv[11]);
    
    
    for (int i = 0; i < argc; ++i) {
        cout << argv[i] << " ";
    }
    cout << endl;
    
    while (!((game.get_pt_02() >= pt_to_win) || (game.get_pt_13() >= pt_to_win))) {
        cout << "Hand " << game.get_hand() << endl;
        game.Shuffle_pack(argv[2]);
        cout << *game.get_dealer() << " deals" << endl;
        game.Deal();
        game.Making_trump();
        game.Play();
    }
    
    game.Print_winning_message(pt_to_win);
    
}

static int find_trick_max(Card trick_card[], const string &trump, const Card &led_card) {
    Card max_card = trick_card[0];
    int max_index = 0;
    
    for (int i = 1; i < TRICK_CARD_SIZE; ++i) {
        if (Card_less(max_card, trick_card[i], led_card, trump)) {
            max_card = trick_card[i];
            max_index = i;
        }
    }
    
    return max_index;
}



