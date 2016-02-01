# -*- coding: utf-8 -*-
"""
Created on Sun Jan 31 22:49:35 2016

@author: cgavi
"""

import gambit

from gambit.nash import ExternalEnumPureSolver
from gambit.nash import ExternalEnumMixedSolver
from gambit.nash import ExternalLogitSolver


player_number = 2
game_title = "The Priority Inflation Game"
strategy_subset = [0, 0.5]
strategy_profiles = [[0, 0], [0, 1], [1, 0], [1, 1]]
payoffs = [[8, 8], [2, 10], [10, 2], [5, 5]]

def build_strategic_game():
    strategies_list = []
    strategies_per_player = len(strategy_subset)
    for strategy_number in range(player_number):
        strategies_list.append(strategies_per_player)
    
    game = gambit.new_table(strategies_list)
    game.title = game_title
    
    define_strategies(game)
    define_payoffs(game)    
    return game

def define_strategies(game):
    for player_index in range(player_number):
        strategy_list = game.players[player_index].strategies
        
        for index, strategy in enumerate(strategy_list):
            strategy.label = "Inflation Ratio " + str(strategy_subset[index])

def define_payoffs(game):
    for index, profile in enumerate(strategy_profiles): 
        payoff_vector = payoffs[index]
  
        for index, payoff_value in enumerate(payoff_vector):
            game[profile][index] = payoff_value
        
def list_player_strategies(game, player_index):  
    strategy_list = game.players[player_index].strategies     
    print 'Player ', player_index, ' has ', len(strategy_list), ' strategies.'      
    print strategy_list

def list_pure_strategy_profiles(game):
   for profile in game.contingencies:
       payoff_string = ""
       for player_index in range(player_number):
           payoff_string += str(game[profile][player_index]) + " "
       
       print "profile ", profile, " payoff_string ", payoff_string

def compute_nash_equilibrium(game, solver):
    result = solver.solve(game)
    print 'solver ', solver 
    print 'result ', type(result), result

game = build_strategic_game()

list_player_strategies(game, 0)
list_pure_strategy_profiles(game)


solver = ExternalEnumPureSolver()
compute_nash_equilibrium(game, solver)

solver = ExternalEnumMixedSolver()
compute_nash_equilibrium(game, solver)

solver = ExternalLogitSolver()
compute_nash_equilibrium(game, solver)
