from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import numpy as np
from scipy.optimize import curve_fit
from sklearn.metrics import root_mean_squared_error

app = FastAPI(title="Cocomo ML Calibration API")

class ProjectHistory(BaseModel):
    ksloc: float
    avg_complexity: float 
    total_em: float        
    actual_effort_pm: float 

class CalibrationResponse(BaseModel):
    new_a: float
    new_b: float
    rmse: float
    projects_analyzed: int

def cocomo_func(X, A, B):

    ksloc, cplx, total_em = X
    E = B + (0.01 * cplx)
    return A * (ksloc ** E) * total_em

@app.post("/api/v1/calibrate", response_model=CalibrationResponse)
def calibrate_model(projects: List[ProjectHistory]):
    if len(projects) < 3:
        raise HTTPException(status_code=400, detail="Not enough data for training. Minimum 3 completed projects.")

    ksloc_list =[]
    cplx_list = []
    em_list = []
    actual_pm_list =[]

    for p in projects:
        ksloc_list.append(p.ksloc)
        cplx_list.append(p.avg_complexity)
        em_list.append(p.total_em)
        actual_pm_list.append(p.actual_effort_pm)

    X = np.array([ksloc_list, cplx_list, em_list])
    y = np.array(actual_pm_list)

    initial_guess =[2.94, 0.91]
    
    bounds = ([0.1, 0.5], [10.0, 1.5])

    try:
        popt, _ = curve_fit(cocomo_func, X, y, p0=initial_guess, bounds=bounds)
        new_A, new_B = popt

        y_pred = cocomo_func(X, new_A, new_B)
        rmse = root_mean_squared_error(y, y_pred)

        return CalibrationResponse(
            new_a=round(float(new_A), 3),
            new_b=round(float(new_B), 3),
            rmse=round(float(rmse), 3),
            projects_analyzed=len(projects)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error training model: {str(e)}")

@app.get("/health")
def health_check():
    return {"status": "ML Service is running"}