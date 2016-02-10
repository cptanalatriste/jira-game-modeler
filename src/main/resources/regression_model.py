import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

from sklearn.cross_validation import *
from sklearn.preprocessing import StandardScaler
from sklearn import linear_model
from sklearn import svm
from sklearn import metrics
from pandas import DataFrame

file_directory = 'C:/Users/cgavi/OneDrive/phd2/jira_data/'
file_name = 'Tester_Behaviour_Board_2_1455034079173.csv'

testers = ['jessicawang', 'likithas', 'minchen07', 'bhaisaab', 'chandanp', 'jayapal',
           'rayeesn', 'sailaja']
data_columns = ['Expected Inflated Fixes', 'Expected Severe Fixes', 
                'Expected Non Severe Fixes']
target_column = 'Next Release Fixes'
strategy_column = 'Inflation Ratio'


def load_game_dataset():
    data_frame = pd.read_csv(file_directory + file_name)
    
    print 'data_frame.columns.values', data_frame.columns.values
    print 'MAX: Next Release Fixes', np.max(data_frame[target_column])
    print 'MIN: Next Release Fixes', np.min(data_frame[target_column])
    print 'MEAN: Next Release Fixes', np.mean(data_frame[target_column])
  
    print 'MAX: ' + strategy_column, np.max(data_frame[strategy_column])
    print 'MIN: ' + strategy_column, np.min(data_frame[strategy_column])
    print 'MEAN: ' + strategy_column, np.mean(data_frame[strategy_column])  
  
    return data_frame
    
def split_dataset(data_frame, scale=True):
    data = data_frame.loc[:, data_columns] 
    target = data_frame.loc[:, target_column]
    
    x_train, x_test, y_train, y_test = train_test_split(data, target,
                                                        test_size = 0.25,
                                                        random_state=33)
                                                        
    if scale:
        print 'Scaling data...'
        scaler_x = StandardScaler().fit(x_train)
        scaler_y = StandardScaler().fit(y_train)
        
        x_train = scaler_x.transform(x_train)
        y_train = scaler_y.transform(y_train)
        x_test = scaler_x.transform(x_test)
        y_test = scaler_y.transform(y_test)
    
    return x_train, y_train, x_test, y_test
                                                        
def train_and_evaluate(regressor, x_train, y_train):
    regressor.fit(x_train, y_train)
    print 'Coefficient of determination on training set: ', regressor.score(x_train, y_train)
    cv = KFold(x_train.shape[0], 5, shuffle=True, random_state=33)
    scores = cross_val_score(regressor, x_train, y_train, cv=cv)
    
    print 'scores', scores
    print 'Average coefficient of determination using 5-fold cross-validation', np.mean(scores)

def create_linear_regressor(x_train, y_train):
    regressor = linear_model.SGDRegressor()
    train_and_evaluate(regressor, x_train, y_train)
    return regressor

def create_svm_regressor(x_train, y_train):
    regressor = svm.SVR(kernel='rbf')
    train_and_evaluate(regressor, x_train, y_train)
    return regressor
    
def measure_performance(x_test, y_test, regressor):
    y_pred = regressor.predict(x_test)
    
    print 'Coefficient of determination: {0:.3f}'.format(
        metrics.r2_score(y_test, y_pred)), '\n'    
                                
def plot_external_event(data_frame, event_column, ax=None):
    values = data_frame[event_column]
    values.hist(bins=50, alpha=0.3, normed=True, ax=ax)
    values.plot(kind='kde', style='k--', ax=ax, title=event_column)
    
    #values.hist(bins=50, alpha=0.3,ax=ax)
    
def plot_strategy(data_frame, x_column=None, y_column=None, ax=None, title=None):
    print title, ": Plotting ", len(data_frame.index), " data points "
    data_frame.plot(kind='line', x=x_column, y=y_column, ax=ax, title=title)

def plot_tester_strategy(tester_list, data_frame, metric):
    for tester_name in tester_list:
        tester_data_frame = load_tester_reports(data_frame, tester_name)
        plot_strategy(data_frame=tester_data_frame, x_column='Release',
              y_column=metric, title= metric + " - "+ tester_name)
  
def load_release_dataset(data_frame):
    release_values = data_frame['Release'].unique()
    developer_productivity_values = []
    dev_productivity_ratio_values = []
    avg_inflation_ratio = []
    var_inflation_ratio = []
    med_inflation_ratio = []
    severity_ratio = []
    
    for release in release_values:
        release_data = data_frame[data_frame['Release'].isin([release])]
        developer_productivity_values.append(release_data['Developer Productivity'].iloc[0])
        dev_productivity_ratio_values.append(release_data['Developer Productivity Ratio'].iloc[0])
        avg_inflation_ratio.append(release_data['Inflation Ratio (mean)'].iloc[0])
        med_inflation_ratio.append(release_data['Inflation Ratio (med)'].iloc[0])
        var_inflation_ratio.append(release_data['Inflation Ration (var)'].iloc[0])
        severity_ratio.append(release_data['Release Severity Ratio'].iloc[0])        
        
    return DataFrame({'Order': range(len(release_values)),
                      'Release': release_values,
                      'Developer Productivity': developer_productivity_values,
                      'Developer Productivity Ratio': dev_productivity_ratio_values,
                      'Inflation Ratio (mean)': avg_inflation_ratio,
                      'Inflation Ratio (med)': med_inflation_ratio,
                      'Inflation Ration (var)': var_inflation_ratio,
                      'Release Severity Ratio': severity_ratio})

def load_tester_reports(data_frame, tester_name):
    tester_data_frame = data_frame[data_frame['Tester'].isin([tester_name])]
    tester_data_frame.reindex()
    return tester_data_frame

data_frame = load_game_dataset()
release_data_frame = load_release_dataset(data_frame)

#Plotting external event data
fig, axes = plt.subplots(8, 1, figsize=(10, 30))
plot_external_event(data_frame, strategy_column, axes[0])
plot_external_event(data_frame, 'Severe Issues', axes[1])
plot_external_event(data_frame, 'Non-Severe Issues Found', axes[2])
plot_external_event(release_data_frame, 'Developer Productivity Ratio', axes[3])
plot_strategy(release_data_frame, 'Release', 'Inflation Ratio (mean)', axes[4])
plot_strategy(release_data_frame, 'Release', 'Inflation Ratio (med)', axes[5])
plot_strategy(release_data_frame, 'Release', 'Inflation Ration (var)', axes[6])
plot_strategy(release_data_frame, 'Release', 'Release Severity Ratio', axes[7])

plot_tester_strategy(testers, data_frame, 'Severe Ratio Reported')
plot_tester_strategy(testers, data_frame, 'Inflation Ratio')

#Creating regression
x_train, y_train, x_test, y_test = split_dataset(data_frame, False)

regressor = create_linear_regressor(x_train, y_train)
print 'regressor.coef_', regressor.coef_
print 'regressor.intercept_ ', regressor.intercept_ 


measure_performance(x_test, y_test, regressor)



