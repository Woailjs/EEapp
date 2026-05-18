from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import analysis, articles
import models
from database import engine

# Create the database tables on startup
models.Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="ElderEscort API",
    description="ElderEscort backend API for fraud detection and articles",
    version="1.1"
)

# Enable CORS for all origins, assuming integration with frontend/Android
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(analysis.router, prefix="/api/v1")
app.include_router(articles.router, prefix="/api/v1")

@app.get("/")
def read_root():
    return {"message": "Welcome to ElderEscort API"}
