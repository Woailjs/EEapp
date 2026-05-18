from sqlalchemy import Column, Integer, String, Boolean, Float, Text
from database import Base

class Article(Base):
    __tablename__ = "articles"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, index=True)
    summary = Column(String)
    content = Column(Text)
