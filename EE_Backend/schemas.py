from pydantic import BaseModel
from typing import List, Optional

# --- Generic Response ---
class BaseResponse(BaseModel):
    code: int
    data: Optional[dict] = None
    message: Optional[str] = None

# --- Analysis ---
class AnalysisRequest(BaseModel):
    text: str

class AnalysisData(BaseModel):
    isFraud: bool
    confidence: float
    warningMessage: Optional[str] = None
    targetArticleId: Optional[int] = None

class AnalysisResponse(BaseModel):
    code: int
    data: AnalysisData

# --- Articles ---
class ArticleItem(BaseModel):
    id: int
    title: str
    summary: str

class ArticleListData(BaseModel):
    list: List[ArticleItem]
    total: int
    page: int
    pageSize: int

class ArticleListResponse(BaseModel):
    code: int
    data: ArticleListData

class ArticleDetail(BaseModel):
    title: str
    content: str

class ArticleDetailResponse(BaseModel):
    code: int
    data: Optional[ArticleDetail] = None
    message: Optional[str] = None
