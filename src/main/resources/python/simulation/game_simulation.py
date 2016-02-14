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

class TestingStrategy:
    pass

class Tester:
    """ A Tester, that reports defect for a Software Version """
    def __init__(self, max_severe=5, min_severe = 10):
        self.max_severe = max_severe
        self.min_severe = min_severe
        self.release_reports = []

    """ Reports a number of defects on the System """
    def report(self):
        # TODO(cgavidia) Implement proper reporting logic, based on the Tester
        # findings and it's learning pattern
        severe = random.randint(1, self.max_severe)
        non_severe = random.randint(1, self.min_severe)
        report = (severe, non_severe)
        self.record(report)
        return report
        
    """ Stores the report made """
    def record(self, report):
        #TODO(cgavidia): I'm not sure if I'm going to use this. Just in case.
        self.release_reports.append(report)

class SoftwareTesting:
    """ Manages the testing of an specific release """
    def __init__(self, tester_team, developer_team):
        self.tester_team = tester_team
        self.developer_team = developer_team
        #TODO(cgavidia): I'm not sure if I'm going to use this. Just in case.
        self.release_reports = []
    
    """ Executes the testing simulation for a number of releases """    
    def test(self, no_of_releases=4):
        for release in range(no_of_releases):
            reports = [tester.report() for tester in self.tester_team]
            self.release_reports.append(reports)
    
    """ Applies fixes to the defects reported """        
    def fix(self):
        fix_reports = [self.developer_team.fix(test_reports) 
                        for test_reports in self.release_reports]
        consolidated_reports = []
        
        for tester_index in range(len(self.tester_team)):
            tester_reports = [release_report[tester_index] 
                                for release_report in fix_reports]
            severe_fixes = [fix_report[0] for fix_report in tester_reports]
            non_severe_fixes = [fix_report[1] for fix_report in tester_reports]
            consolidated_report = sum(severe_fixes), sum(non_severe_fixes)
            consolidated_reports.append(consolidated_report)
            
        return consolidated_reports        

if __name__ == "__main__":
    dev_team = DeveloperTeam()
    tester_team = [Tester(), Tester()]

    product_testing = SoftwareTesting(tester_team, dev_team)
    product_testing.test(2)
    fixes = product_testing.fix()
    
    print 'fixes ', fixes  
    