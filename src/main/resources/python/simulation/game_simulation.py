# -*- coding: utf-8 -*-
"""
Created on Sat Feb 13 18:11:45 2016

@author: Carlos G. Gavidia
"""

import random

class DeveloperTeam:
    """ Giving a list of reports, it returns how many of them got fixed """
    def fix(self, tester_reports):
        #TODO(cgavidia): We might need a report class.
        #TODO(cgavidia): This is a dummy implementation. We need to consider
        # the priorities and the stochastic dev team productivity.
        no_testers = len(tester_reports)
        fix_reports = [[0, 0] for _ in range(no_testers)]
        productivity = random.randint(5, 10)
    
        print 'tester_reports ', tester_reports        
        print 'productivity ', productivity

        fixes_delivered = 0

        while fixes_delivered != productivity:
            selected_tester = random.randint(0, no_testers - 1)
            tester_report = tester_reports[selected_tester]
            fixes_for_tester = fix_reports[selected_tester]

            if tester_report[0] > fixes_for_tester[0]:
                fixes_for_tester[0] += 1
                fixes_delivered += 1
            elif tester_report[1] > fixes_for_tester[1]:
                fixes_for_tester[1] += 1
                fixes_delivered += 1

        return fix_reports

class MaxMinTestingStrategy:
    """ Defines a particular strategy for testing on a release """
    def __init__(self, max_severe=5, max_non_severe = 10):
        self.max_severe = max_severe
        self.max_non_severe = max_non_severe
        
    def report(self):
        # TODO(cgavidia) Implement proper reporting logic, based on the Tester
        # findings and it's learning pattern
        severe = random.randint(1, self.max_severe)
        non_severe = random.randint(1, self.max_non_severe)
        return (severe, non_severe)
        
class Tester:
    """ A Tester, that reports defect for a Software Version """
    def __init__(self, testing_strategy):
        self.testing_strategy = testing_strategy
        self.reset()
    
    """ Clears the testing history """
    def reset(self):
        self.release_reports = []
        self.fix_reports = []

    """ Reports a number of defects on the System """
    def report(self):
        # TODO(cgavidia) Implement proper reporting logic, based on the Tester
        # findings and it's learning pattern
        report = self.testing_strategy.report()
        return report
        
    """ Stores the report made """
    def record(self, test_reports, fix_reports):
        #TODO(cgavidia): I'm not sure if I'm going to use this. Just in case.
        self.release_reports.append(test_reports)
        self.fix_reports.append(fix_reports)

class SoftwareTesting:
    """ Manages the testing of an specific release """
    def __init__(self, tester_team, developer_team):
        self.tester_team = tester_team
        self.developer_team = developer_team
        #TODO(cgavidia): I'm not sure if I'm going to use this. Just in case.
        self.release_reports = []
        self.fix_reports = []
    
    """ Executes the testing simulation for a number of releases. It includes now
    the fix procedure"""    
    def test_and_fix(self, no_of_releases=4):
        #TODO(cgavidia): Evaluate if this is convenient: To first execute ALL
        # the reporting and then ALL the fixing.
        for release in range(no_of_releases):
            test_reports = [tester.report() for tester in self.tester_team]
            self.release_reports.append(test_reports)
            
            fix_reports = self.developer_team.fix(test_reports)
            self.fix_reports.append(fix_reports)
            
            for index, tester in enumerate(self.tester_team):
                tester.record(test_reports[index], fix_reports[index])
                
    
    """ Applies fixes to the defects reported """        
    def consolidate_report(self):
        consolidated_reports = []
        
        for index, tester in enumerate(self.tester_team):
            tester_fix_reports = [release_fix_report[index] 
                                for release_fix_report in self.fix_reports]
                                    
            severe_fixes = [fix_report[0] for fix_report in tester_fix_reports]
            non_severe_fixes = [fix_report[1] for fix_report in tester_fix_reports]
            
            consolidated_report = sum(severe_fixes), sum(non_severe_fixes)
            consolidated_reports.append(consolidated_report)            
            
        return consolidated_reports        

if __name__ == "__main__":
    dev_team = DeveloperTeam()
    tester_1_strategy = MaxMinTestingStrategy() 
    tester_2_strategy = MaxMinTestingStrategy()
    
    tester_1 = Tester(tester_1_strategy)
    tester_2 = Tester(tester_2_strategy)

    tester_team = [tester_1, tester_2]

    product_testing = SoftwareTesting(tester_team, dev_team)
    product_testing.test_and_fix(2)
    fixes = product_testing.consolidate_report()
    
    print 'fixes ', fixes  
    print 'tester_1.release_reports  ', tester_1.release_reports  
    print 'tester_1.fix_reports  ', tester_1.fix_reports  
    print 'tester_2.release_reports  ', tester_2.release_reports  
    print 'tester_2.fix_reports  ', tester_2.fix_reports  
    