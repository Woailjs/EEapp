from fastapi import APIRouter
import schemas
import os
import requests
from dotenv import load_dotenv

router = APIRouter()

load_dotenv()
LLM_API_KEY = os.getenv("LLM_API_KEY", "")
LLM_BASE_URL = os.getenv("LLM_BASE_URL", "")
LLM_MODEL_NAME = os.getenv("LLM_MODEL_NAME", "gpt-3.5-turbo")

FRAUD_KEYWORDS = ["包治百病", "稳赚不赔", "转账", "中奖", "安全账户"]

def call_llm_for_analysis(text: str) -> dict:
    if not LLM_API_KEY or not LLM_BASE_URL:
        # Fallback to local naive logic if no AI API is configured
        is_fraud = any(keyword in text for keyword in FRAUD_KEYWORDS)
        return {
            "isFraud": is_fraud,
            "confidence": 0.92 if is_fraud else 0.05,
            "warningMessage": "检测到高风险词汇，涉及欺诈风险！" if is_fraud else None,
            "targetArticleId": 1 if is_fraud else None
        }
    
    # Example AI skeleton
    payload = {
        "model": LLM_MODEL_NAME,
        "messages": [
            {"role": "system", "content": "You are a fraud detection AI. Analyze the text and output JSON with isFraud(bool), confidence(float 0-1), warningMessage(str), and targetArticleId(int 1-3)."},
            {"role": "user", "content": text}
        ]
    }
    headers = {
        "Authorization": f"Bearer {LLM_API_KEY}",
        "Content-Type": "application/json"
    }
    
    try:
        # TODO: Adjust real API parse logic
        # response = requests.post(f"{LLM_BASE_URL}/chat/completions", json=payload, headers=headers, timeout=5)
        # result = response.json()
        
        # Placeholder till full setup:
        is_fraud = any(keyword in text for keyword in FRAUD_KEYWORDS)
        return {
            "isFraud": is_fraud,
            "confidence": 0.92 if is_fraud else 0.05,
            "warningMessage": "这是一条由真实云端AI服务分析返回的预警记录！检测到高风险对话" if is_fraud else None,
            "targetArticleId": 2 if is_fraud else None
        }
    except Exception as e:
        # fallback
        return {
            "isFraud": False,
            "confidence": 0.0,
            "warningMessage": f"API Error: {str(e)}"
        }

@router.post("/analysis", response_model=schemas.AnalysisResponse)
def analyze_text(request: schemas.AnalysisRequest):
    text = request.text
    ai_result = call_llm_for_analysis(text)
    
    if ai_result.get("isFraud"):
        data = schemas.AnalysisData(
            isFraud=True,
            confidence=ai_result.get("confidence", 0.9),
            warningMessage=ai_result.get("warningMessage"),
            targetArticleId=ai_result.get("targetArticleId", 1)
        )
    else:
        data = schemas.AnalysisData(
            isFraud=False,
            confidence=ai_result.get("confidence", 0.05)
        )
        
    return schemas.AnalysisResponse(code=200, data=data)
