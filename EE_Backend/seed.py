from database import SessionLocal, engine
import models

def seed_db():
    models.Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    
    if db.query(models.Article).count() == 0:
        articles = [
            models.Article(
                title="警惕“包治百病”神药骗局",
                summary="不法分子常利用“包治百病”等夸大宣传语欺骗老年人购买高价保健品，了解常见骗术，守护您的健康与财产。",
                content="近年来，一些不法分子利用老年人对健康的渴望，推销所谓的“神药”。\n\n请记住：世上没有包治百病的神药。如果有人告诉你某种药物什么病都能治好，那一定是骗局。"
            ),
            models.Article(
                title="假冒公检法诈骗防范指南",
                summary="如果接到自称公安、法院的电话说你涉嫌洗黑钱，要求转账到安全账户，千万别信！",
                content="公检法机关办案绝不会通过电话要求转账，更不存在所谓的“安全账户”。\n\n遇到此类电话，请直接挂机并拨打110核实。"
            ),
            models.Article(
                title="高息理财？当心血本无归",
                summary="宣称“稳赚不赔”、“高回报”的理财产品往往是精心设计的庞氏骗局。",
                content="不要轻信非正规渠道推荐的理财产品。\n\n所有声称“零风险、高收益”的项目，都是为了骗取您的本金。"
            )
        ]
        db.add_all(articles)
        db.commit()
    
    db.close()

if __name__ == "__main__":
    print("Seeding database...")
    seed_db()
    print("Database seeding completed.")
