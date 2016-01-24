import pandas as pd

file_directory = 'C:/Users/cgavi/OneDrive/phd2/jira_data/'
file_name = 'Tester_Behaviour_Board_2_1453675537124.csv'

agent_plays = pd.read_csv(file_directory + file_name)
print agent_plays