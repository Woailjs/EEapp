from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.orm import Session
from typing import Optional
import models, schemas
from database import get_db

router = APIRouter()

@router.get("/articles", response_model=schemas.ArticleListResponse)
def get_articles(
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100),
    db: Session = Depends(get_db)
):
    skip = (page - 1) * pageSize
    total = db.query(models.Article).count()
    db_articles = db.query(models.Article).offset(skip).limit(pageSize).all()
    
    list_items = [
        schemas.ArticleItem(id=a.id, title=a.title, summary=a.summary)
        for a in db_articles
    ]
    
    data = schemas.ArticleListData(
        list=list_items,
        total=total,
        page=page,
        pageSize=pageSize
    )
    return schemas.ArticleListResponse(code=200, data=data)

@router.get("/articles/{article_id}", response_model=schemas.ArticleDetailResponse)
def get_article(article_id: int, db: Session = Depends(get_db)):
    article = db.query(models.Article).filter(models.Article.id == article_id).first()
    
    if not article:
        return schemas.ArticleDetailResponse(
            code=404, 
            message="文章未找到"
        )
        
    data = schemas.ArticleDetail(
        title=article.title,
        content=article.content
    )
    return schemas.ArticleDetailResponse(code=200, data=data)
