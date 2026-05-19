import MockAdapter from 'axios-mock-adapter'
import request from '../api/request.js'

const mock = new MockAdapter(request, { delayResponse: 800 })

// 拦截规则 1: POST /api/v1/analysis - 分析文本是否欺诈
mock.onPost('/api/v1/analysis').reply((config) => {
  const body = JSON.parse(config.data)
  const text = body.text || ''
  const isFraud = text.includes('心绞痛') || text.includes('免费')

  if (isFraud) {
    return [200, {
      code: 200,
      data: {
        isFraud: true,
        confidence: 0.95,
        warningMessage: '检测到虚假夸大宣传！',
        targetArticleId: text.includes('心绞痛') ? 4 : 1,
      },
    }]
  }

  return [200, {
    code: 200,
    data: {
      isFraud: false,
      confidence: 0.02,
    },
  }]
})

// 拦截规则 2: GET /api/v1/articles - 获取科普文章列表（分页）
mock.onGet('/api/v1/articles').reply((config) => {
  const page = parseInt(config.params?.page) || 1
  const pageSize = parseInt(config.params?.pageSize) || 10

  const allArticles = [
    {
      id: 1,
      title: '警惕"包治百病"骗局',
      summary: '不法分子常利用"包治百病"等夸大宣传语欺骗老年人购买高价保健品，了解常见骗术，守护您的健康与财产。',
    },
    {
      id: 2,
      title: '免费讲座背后的陷阱',
      summary: '以"免费讲座"、"免费体检"为名的诈骗活动频发，不法分子借机推销假冒伪劣产品，一定要提高警惕。',
    },
    {
      id: 3,
      title: '冒充公检法诈骗防范指南',
      summary: '诈骗分子冒充公安、检察院、法院工作人员实施诈骗，了解正确的应对方法，避免上当受骗。',
    },
    {
      id: 4,
      title: '心绞痛的识别与防治',
      summary: '心绞痛是中老年人常见的心血管疾病信号，了解其症状和正确应对方法至关重要。',
    },
  ]

  const start = (page - 1) * pageSize
  const list = allArticles.slice(start, start + pageSize)

  return [200, {
    code: 200,
    data: {
      list,
      total: allArticles.length,
      page,
      pageSize,
    },
  }]
})

// 拦截规则 3: GET /api/v1/articles/:id - 获取文章详情
mock.onGet(/\/api\/v1\/articles\/\d+$/).reply((config) => {
  const id = parseInt(config.url.match(/\/(\d+)$/)[1])

  const articles = {
    1: {
      title: '警惕"包治百病"骗局',
      content: '近年来，一些不法分子利用老年人对健康的渴望，打出"包治百病"、"药到病除"等虚假广告，欺骗老年人购买高价保健品。\n\n这些所谓的"神药"往往只是普通的食品或保健品，没有任何治疗功效。犯罪分子通过免费讲座、赠送小礼品等方式吸引老年人参加，再通过虚假宣传、恐吓等手段诱导购买。\n\n请记住：世上没有包治百病的神药。遇到身体不适，一定要去正规医院就诊，不要轻信街头广告和陌生人的推荐。\n\n如果遇到类似情况，请及时拨打 12315 消费者投诉举报电话，或向当地公安机关报案。',
    },
    2: {
      title: '免费讲座背后的陷阱',
      content: '"免费讲座"、"免费体检"、"免费旅游"——这些看似天上掉馅饼的好事，背后往往隐藏着精心设计的骗局。\n\n不法分子通常以"关爱老年人健康"为名义，组织各类免费活动。在活动中，他们会安排"专家"进行健康讲座，夸大某些产品的功效，制造恐慌情绪，最终诱导老年人购买高价产品。\n\n请警惕以下套路：\n1. 以免费为噱头吸引参与\n2. 安排"托儿"营造抢购氛围\n3. 限制时间，制造紧迫感\n4. 夸大产品功效，虚假宣传\n\n天下没有免费的午餐。遇到此类活动，请多与子女沟通，不要轻易掏钱购买。',
    },
    3: {
      title: '冒充公检法诈骗防范指南',
      content: '冒充公检法诈骗是近年来高发的电信诈骗类型之一。诈骗分子冒充公安、检察院、法院等国家机关工作人员，以"涉嫌犯罪"、"账户异常"等为由，要求受害人转账汇款。\n\n请牢记以下要点：\n1. 公检法机关不会通过电话办案，更不会要求转账汇款\n2. 不会索要银行卡号、密码、验证码等个人信息\n3. 不会设立所谓的"安全账户"\n4. 如接到此类电话，立即挂断，不要理会\n\n真正的公检法工作人员执行公务时，会出示相关证件和法律文书，绝不会通过电话要求您转账。遇到可疑电话，请立即挂断并拨打 110 报警。',
    },
    4: {
      title: '心绞痛的识别与防治',
      content: '心绞痛是由于冠状动脉供血不足导致心肌缺血缺氧而引起的胸部疼痛，是中老年人常见的心血管疾病危险信号。\n\n典型症状：\n1. 胸骨后或心前区压榨性疼痛，可放射至左肩、左臂内侧\n2. 疼痛持续时间一般为3-5分钟，很少超过15分钟\n3. 常在体力劳动、情绪激动、饱餐后诱发\n4. 休息或含服硝酸甘油后可缓解\n\n紧急处理：\n1. 立即停止活动，坐下或半卧位休息\n2. 舌下含服硝酸甘油（如医生已处方）\n3. 如5分钟后症状不缓解，立即拨打120急救电话\n\n请务必重视：任何胸痛症状都应及时就医，不要自行诊断或轻信偏方治疗。许多不法分子利用老年人对心绞痛的恐惧，推销所谓的"特效药"或"秘方"，这些产品不仅无效，还可能延误治疗，危及生命！',
    },
  }

  const article = articles[id]
  if (article) {
    return [200, { code: 200, data: article }]
  }
  return [404, { code: 404, message: '文章未找到' }]
})
