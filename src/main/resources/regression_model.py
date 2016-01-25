import pandas as pd
import numpy as np

from sklearn.cross_validation import *
from sklearn.preprocessing import StandardScaler
from sklearn import linear_model
from sklearn import svm
from sklearn import metrics

file_directory = 'C:/Users/cgavi/OneDrive/phd2/jira_data/'
file_name = 'Tester_Behaviour_Board_2_1453682143281.csv'

def load_dataset():
    data_frame = pd.read_csv(file_directory + file_name)
    
    print 'data_frame.columns.values', data_frame.columns.values
    print 'MAX: Next Release Fixes', np.max(data_frame['Next Release Fixes'])
    print 'MIN: Next Release Fixes', np.min(data_frame['Next Release Fixes'])
    print 'MEAN: Next Release Fixes', np.mean(data_frame['Next Release Fixes'])

    return data_frame
    
def preprocess_data(data_frame):
    data = data_frame.ix[:, 'Developer Productivity' :'External Inflation']
 
    target = data_frame.ix[:, 'Next Release Fixes']
    x_train, x_test, y_train, y_test = train_test_split(data, target,
                                                        test_size = 0.25,
                                                        random_state=33)
                                                        
    scaler_x = StandardScaler().fit(x_train)
    scaler_y = StandardScaler().fit(y_train)
    
    x_train = scaler_x.transform(x_train)
    y_train = scaler_y.transform(y_train)
    x_test = scaler_x.transform(x_test)
    y_test = scaler_y.transform(y_test)
    
    return x_train, y_train, x_test, y_test
                                                        
def train_and_evaluate(regressor, x_train, y_train):
    regressor.fit(x_train, y_train)
    print 'Coefficient of determination on training set: ', regressor.score(x_train,
                                                                      y_train)
    cv = KFold(x_train.shape[0], 5, shuffle=True, random_state=33)
    scores = cross_val_score(regressor, x_train, y_train, cv=cv)
    print 'Average coefficient of determination using 5-fold cross-validation', np.mean(scores)

def create_linear_regressor(x_train, y_train):
    regressor = linear_model.SGDRegressor(loss='squared_loss', penalty=None, 
                                        random_state=42)
    train_and_evaluate(regressor, x_train, y_train)
    return regressor

def create_svm_regressor(x_train, y_train):
    #Using RBF and Poly degrades greatly the quality of the regressor. Linear 
    #works fine
    regressor = svm.SVR(kernel='rbf')
    train_and_evaluate(regressor, x_train, y_train)
    return regressor
    
def measure_performance(x_test, y_test, regressor):
    y_pred = regressor.predict(x_test)
    
    print 'Coefficient of determination: {0:.3f}'.format(
        metrics.r2_score(y_test, y_pred)), '\n'    
                                

data_frame = load_dataset()
preprocess_data(data_frame)

x_train, y_train, x_test, y_test = preprocess_data(data_frame)

regressor = create_regressor(x_train, y_train)
print 'regressor.coef_', regressor.coef_

#regressor = create_svm_regressor(x_train, y_train)
measure_performance(x_test, y_test, regressor)



