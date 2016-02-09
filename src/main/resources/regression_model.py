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
file_name = 'Tester_Behaviour_Board_2_1454688181389.csv'

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
                                
def plot_external_event(data_frame, event_column, ax):
    values = data_frame[event_column]
    values.hist(bins=50, alpha=0.3, normed=True, ax=ax)
    values.plot(kind='kde', style='k--', ax=ax, title=event_column)
    
    #values.hist(bins=50, alpha=0.3,ax=ax)
    
def plot_strategy(data_frame, x_column, y_column, ax):
    data_frame.plot(kind='line', x=x_column, y=y_column)
    
def load_release_dataset(data_frame):
    release_values = data_frame['Release'].unique()
    developer_productivity_values = []
    dev_productivity_ratio_values = []
    avg_inflation_ratio = []
    var_inflation_ratio = []
    med_inflation_ratio = []

    
    for release in release_values:
        release_data = data_frame[data_frame['Release'].isin([release])]
        developer_productivity_values.append(release_data['Developer Productivity'].iloc[0])
        dev_productivity_ratio_values.append(release_data['Developer Productivity Ratio'].iloc[0])
        avg_inflation_ratio.append(release_data['Inflation Ratio (mean)'].iloc[0])
        med_inflation_ratio.append(release_data['Inflation Ratio (med)'].iloc[0])
        var_inflation_ratio.append(release_data['Inflation Ration (var)'].iloc[0])
                
    return DataFrame({'Order': range(len(release_values)),
                      'Release': release_values,
                      'Developer Productivity': developer_productivity_values,
                      'Developer Productivity Ratio': dev_productivity_ratio_values,
                      'Inflation Ratio (mean)': avg_inflation_ratio,
                      'Inflation Ratio (med)': med_inflation_ratio,
                      'Inflation Ration (var)': var_inflation_ratio})


data_frame = load_game_dataset()
release_data_frame = load_release_dataset(data_frame)

#Plotting external event data
fig, axes = plt.subplots(7, 1, figsize=(15, 12))
plot_external_event(data_frame, strategy_column, axes[0])
plot_external_event(data_frame, 'Severe Issues', axes[1])
plot_external_event(data_frame, 'Non-Severe Issues Found', axes[2])
plot_external_event(release_data_frame, 'Developer Productivity Ratio', axes[3])
plot_strategy(release_data_frame, 'Order', 'Inflation Ratio (mean)', axes[4])
plot_strategy(release_data_frame, 'Order', 'Inflation Ratio (med)', axes[5])
plot_strategy(release_data_frame, 'Order', 'Inflation Ration (var)', axes[6])

#Creating regression
x_train, y_train, x_test, y_test = split_dataset(data_frame, False)

regressor = create_linear_regressor(x_train, y_train)
print 'regressor.coef_', regressor.coef_
print 'regressor.intercept_ ', regressor.intercept_ 


measure_performance(x_test, y_test, regressor)



