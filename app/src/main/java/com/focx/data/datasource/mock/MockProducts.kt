package com.focx.data.datasource.mock

import com.focx.domain.entity.Product

val mockProducts = listOf(
    Product(
        id = "1",
        name = "Apple Vision Pro",
        description = "Apple Vision Pro is a spatial computer that blends digital content and apps with your physical space, and lets you navigate using your eyes, hands, and voice.",
        price = 3499.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&h=400&fit=crop"
        ),
        sellerId = "seller1",
        sellerName = "TechStore Official",
        category = "Electronics",
        stock = 50,
        salesCount = 120,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express", "DHL"),
        specifications = mapOf("SoC" to "Apple M2", "Display" to "Micro-OLED", "Resolution" to "23 million pixels"),
        rating = 4.9f,
        reviewCount = 85
    ),
    Product(
        id = "2",
        name = "Tesla CyberTruck",
        description = "A rugged and futuristic electric pickup truck with a stainless steel exoskeleton and armored glass.",
        price = 79990.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=400&h=400&fit=crop"
        ),
        sellerId = "seller2",
        sellerName = "Future Motors",
        category = "Automotive",
        stock = 10,
        salesCount = 5,
        shippingFrom = "Austin, USA",
        shippingTo = listOf("USA", "Canada"),
        shippingMethods = listOf("Tesla Delivery"),
        specifications = mapOf("Range" to "340 miles", "0-60 mph" to "2.9 seconds", "Towing Capacity" to "14,000 lbs"),
        rating = 4.5f,
        reviewCount = 3
    ),
    Product(
        id = "3",
        name = "Anker 737 Power Bank",
        description = "A high-capacity power bank with 140W output, perfect for charging laptops, tablets, and phones on the go.",
        price = 149.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&h=400&fit=crop"
        ),
        sellerId = "seller3",
        sellerName = "EV Accessories",
        category = "Electronics",
        stock = 200,
        salesCount = 850,
        shippingFrom = "Beijing, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("JD Logistics", "SF Express"),
        specifications = mapOf("Capacity" to "24,000mAh", "Max Output" to "140W", "Ports" to "2x USB-C, 1x USB-A"),
        rating = 4.8f,
        reviewCount = 450
    ),
    Product(
        id = "4",
        name = "LEGO Star Wars Millennium Falcon",
        description = "The ultimate LEGO Star Wars Millennium Falcon model, featuring intricate details and a host of characters.",
        price = 849.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1606400082777-ef05f3c5cde2?w=400&h=400&fit=crop"
        ),
        sellerId = "seller4",
        sellerName = "Brick World",
        category = "Toys",
        stock = 30,
        salesCount = 15,
        shippingFrom = "Billund, Denmark",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("DHL", "FedEx"),
        specifications = mapOf("Pieces" to "7,541", "Minifigures" to "10", "Dimensions" to "84cm x 56cm x 21cm"),
        rating = 4.9f,
        reviewCount = 25
    ),
    Product(
        id = "5",
        name = "The Lord of the Rings Trilogy",
        description = "A special edition hardcover box set of J.R.R. Tolkien's epic fantasy trilogy.",
        price = 120.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400&h=400&fit=crop"
        ),
        sellerId = "seller5",
        sellerName = "Bookworm Haven",
        category = "Books",
        stock = 100,
        salesCount = 300,
        shippingFrom = "London, UK",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("Royal Mail", "UPS"),
        specifications = mapOf("Author" to "J.R.R. Tolkien", "Format" to "Hardcover", "Pages" to "1,216"),
        rating = 5.0f,
        reviewCount = 150
    ),
    Product(
        id = "6",
        name = "Lululemon Align High-Rise Pant",
        description = "Buttery-soft and weightless, the Align collection is designed for ultimate comfort during yoga or everyday wear.",
        price = 98.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=400&h=400&fit=crop"
        ),
        sellerId = "seller6",
        sellerName = "Active Lifestyle Co.",
        category = "Fashion",
        stock = 150,
        salesCount = 1200,
        shippingFrom = "Vancouver, Canada",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("Canada Post", "DHL"),
        specifications = mapOf(
            "Material" to "Nulu™ Fabric",
            "Fit" to "High-Rise, 28\" Length",
            "Features" to "Hidden waistband pocket"
        ),
        rating = 4.7f,
        reviewCount = 800
    ),
    Product(
        id = "7",
        name = "Dyson V15 Detect Absolute",
        description = "A cordless vacuum with laser illumination to reveal microscopic dust and a sensor that adapts power to the debris detected.",
        price = 749.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&h=400&fit=crop"
        ),
        sellerId = "seller7",
        sellerName = "Home Essentials",
        category = "Home Goods",
        stock = 40,
        salesCount = 90,
        shippingFrom = "Malmesbury, UK",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("UPS", "FedEx"),
        specifications = mapOf("Suction Power" to "240AW", "Run Time" to "Up to 60 mins", "Bin Volume" to "0.76L"),
        rating = 4.9f,
        reviewCount = 60
    ),
    Product(
        id = "8",
        name = "Sony WH-1000XM5 Wireless Headphones",
        description = "Industry-leading noise canceling headphones with a new design and even better sound quality.",
        price = 399.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&h=400&fit=crop"
        ),
        sellerId = "seller1",
        sellerName = "TechStore Official",
        category = "Electronics",
        stock = 80,
        salesCount = 600,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        specifications = mapOf(
            "Noise Cancellation" to "Industry Leading",
            "Battery Life" to "30 hours",
            "Connectivity" to "Bluetooth 5.2"
        ),
        rating = 4.8f,
        reviewCount = 400
    ),
    Product(
        id = "9",
        name = "DJI Mini 3 Pro",
        description = "A lightweight and powerful drone with tri-directional obstacle sensing and 4K HDR video capabilities.",
        price = 759.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&h=400&fit=crop"
        ),
        sellerId = "seller9",
        sellerName = "Gadget Hub",
        category = "Electronics",
        stock = 60,
        salesCount = 250,
        shippingFrom = "Shenzhen, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express", "DHL"),
        specifications = mapOf("Weight" to "249g", "Max Video Resolution" to "4K/60fps", "Flight Time" to "34 mins"),
        rating = 4.9f,
        reviewCount = 180
    ),
    Product(
        id = "10",
        name = "Herman Miller Aeron Chair",
        description = "An iconic ergonomic office chair that provides total spinal support and adjusts to your body's posture.",
        price = 1645.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1606400082777-ef05f3c5cde2?w=400&h=400&fit=crop"
        ),
        sellerId = "seller10",
        sellerName = "Office Comforts Inc.",
        category = "Furniture",
        stock = 20,
        salesCount = 40,
        shippingFrom = "Zeeland, USA",
        shippingTo = listOf("USA", "Canada", "Europe"),
        shippingMethods = listOf("FedEx Freight"),
        rating = 5.0f,
        reviewCount = 30
    ),
    Product(
        id = "11",
        name = "The North Face 1996 Retro Nuptse Jacket",
        description = "A boxy, down-filled jacket with a stowable hood and the original shiny ripstop fabric.",
        price = 320.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400&h=400&fit=crop"
        ),
        sellerId = "seller11",
        sellerName = "Outdoor Gear Supply",
        category = "Fashion",
        stock = 120,
        salesCount = 950,
        shippingFrom = "Guangzhou, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express", "ZTO Express"),
        rating = 4.8f,
        reviewCount = 700
    ),
    Product(
        id = "12",
        name = "Samsung Odyssey G9 Gaming Monitor",
        description = "A 49-inch super ultra-wide curved gaming monitor with a 1000R curvature for immersive gameplay.",
        price = 1599.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=400&h=400&fit=crop"
        ),
        sellerId = "seller1",
        sellerName = "TechStore Official",
        category = "Electronics",
        stock = 25,
        salesCount = 60,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.7f,
        reviewCount = 45
    ),
    Product(
        id = "13",
        name = "Instant Pot Duo Plus 9-in-1 Electric Pressure Cooker",
        description = "A versatile kitchen appliance that functions as a pressure cooker, slow cooker, rice cooker, steamer, and more.",
        price = 129.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&h=400&fit=crop"
        ),
        sellerId = "seller7",
        sellerName = "Home Essentials",
        category = "Home Goods",
        stock = 150,
        salesCount = 1500,
        shippingFrom = "Suzhou, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("Deppon Express"),
        rating = 4.8f,
        reviewCount = 1100
    ),
    Product(
        id = "14",
        name = "Canon EOS R5 Mirrorless Camera",
        description = "A full-frame mirrorless camera with 8K video, 45MP photos, and advanced autofocus capabilities.",
        price = 3899.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&h=400&fit=crop"
        ),
        sellerId = "seller9",
        sellerName = "Gadget Hub",
        category = "Electronics",
        stock = 30,
        salesCount = 80,
        shippingFrom = "Shenzhen, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.9f,
        reviewCount = 65
    ),
    Product(
        id = "15",
        name = "All-Clad D3 Stainless Steel Cookware Set",
        description = "A 10-piece set of high-performance stainless steel cookware, handcrafted in the USA.",
        price = 699.95,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&h=400&fit=crop"
        ),
        sellerId = "seller7",
        sellerName = "Home Essentials",
        category = "Home Goods",
        stock = 50,
        salesCount = 200,
        shippingFrom = "Canonsburg, USA",
        shippingTo = listOf("USA", "Canada"),
        shippingMethods = listOf("UPS Ground"),
        rating = 4.9f,
        reviewCount = 150
    ),
    Product(
        id = "16",
        name = "Nike Air Force 1 '07",
        description = "The classic basketball shoe that redefined sneaker culture, with crisp leather and a timeless design.",
        price = 100.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1606400082777-ef05f3c5cde2?w=400&h=400&fit=crop"
        ),
        sellerId = "seller6",
        sellerName = "Active Lifestyle Co.",
        category = "Fashion",
        stock = 300,
        salesCount = 2500,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express", "YTO Express"),
        rating = 4.8f,
        reviewCount = 1800
    ),
    Product(
        id = "17",
        name = "Rimowa Classic Cabin Suitcase",
        description = "A timeless aluminum suitcase with high-end functionality, featuring a durable design and smooth-rolling wheels.",
        price = 1280.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400&h=400&fit=crop"
        ),
        sellerId = "seller12",
        sellerName = "Luxury Travel Goods",
        category = "Travel",
        stock = 20,
        salesCount = 50,
        shippingFrom = "Cologne, Germany",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("DHL Express"),
        rating = 4.9f,
        reviewCount = 40
    ),
    Product(
        id = "18",
        name = "Logitech MX Master 3S Wireless Mouse",
        description = "An advanced wireless mouse with an 8K DPI sensor, quiet clicks, and MagSpeed electromagnetic scrolling.",
        price = 99.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=400&fit=crop"
        ),
        sellerId = "seller1",
        sellerName = "TechStore Official",
        category = "Electronics",
        stock = 150,
        salesCount = 1300,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.9f,
        reviewCount = 1000
    ),
    Product(
        id = "19",
        name = "Adidas Ultraboost 22 Running Shoes",
        description = "Running shoes with incredible energy return and a supportive fit, designed for high-performance running.",
        price = 180.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&h=400&fit=crop"
        ),
        sellerId = "seller6",
        sellerName = "Active Lifestyle Co.",
        category = "Fashion",
        stock = 100,
        salesCount = 900,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.8f,
        reviewCount = 700
    ),
    Product(
        id = "20",
        name = "Nintendo Switch - OLED Model",
        description = "7-inch OLED screen, a wide adjustable stand, a dock with a wired LAN port, 64 GB of internal storage, and enhanced audio.",
        price = 349.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=400&h=400&fit=crop"
        ),
        sellerId = "seller8",
        sellerName = "Brick World",
        category = "Electronics",
        stock = 150,
        salesCount = 2200,
        shippingFrom = "Shenzhen, China",
        shippingTo = listOf("Worldwide", "International"),
        shippingMethods = listOf("SF Express", "DHL"),
        rating = 4.9f,
        reviewCount = 1800
    ),
    Product(
        id = "21",
        name = "Anker PowerCore 26800 Portable Charger",
        description = "A massive 26800mAh capacity portable charger with dual Micro USB input and PowerIQ technology for high-speed charging.",
        price = 65.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&h=400&fit=crop"
        ),
        sellerId = "seller3",
        sellerName = "EV Accessories",
        category = "Electronics",
        stock = 300,
        salesCount = 4500,
        shippingFrom = "Beijing, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("JD Logistics", "SF Express"),
        rating = 4.8f,
        reviewCount = 3200
    ),
    Product(
        id = "22",
        name = "Breville Barista Express Espresso Machine",
        description = "Create great-tasting espresso in less than a minute. The Barista Express allows you to grind the beans right before extraction.",
        price = 699.95,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=400&h=400&fit=crop"
        ),
        sellerId = "seller7",
        sellerName = "Home Essentials",
        category = "Home Goods",
        stock = 25,
        salesCount = 300,
        shippingFrom = "Suzhou, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("Deppon Express"),
        rating = 4.7f,
        reviewCount = 250
    ),
    Product(
        id = "23",
        name = "Garmin Fenix 7X Sapphire Solar Multisport GPS Watch",
        description = "Solar charging, scratch-resistant Power Sapphire lens, and a resilient titanium bezel. Advanced training features and health monitoring.",
        price = 999.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&h=400&fit=crop"
        ),
        sellerId = "seller9",
        sellerName = "Gadget Hub",
        category = "Electronics",
        stock = 40,
        salesCount = 200,
        shippingFrom = "Shenzhen, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.9f,
        reviewCount = 180
    ),
    Product(
        id = "24",
        name = "Patagonia Classic Retro-X Fleece Jacket",
        description = "Windproof, warm and with a soft, fleecy exterior. Made with 50% recycled polyester fleece. Fair Trade Certified™ sewn.",
        price = 199.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1606400082777-ef05f3c5cde2?w=400&h=400&fit=crop"
        ),
        sellerId = "seller6",
        sellerName = "Active Lifestyle Co.",
        category = "Fashion",
        stock = 80,
        salesCount = 600,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.8f,
        reviewCount = 500
    ),
    Product(
        id = "25",
        name = "Kindle Paperwhite Signature Edition",
        description = "32 GB storage, a 6.8” display, wireless charging, and an auto-adjusting front light. Reads like real paper, even in bright sunlight.",
        price = 189.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400&h=400&fit=crop"
        ),
        sellerId = "seller5",
        sellerName = "Bookworm Haven",
        category = "Electronics",
        stock = 200,
        salesCount = 1800,
        shippingFrom = "Hangzhou, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("ZTO Express"),
        rating = 4.9f,
        reviewCount = 1500
    ),
    Product(
        id = "26",
        name = "GoPro HERO11 Black Action Camera",
        description = "Waterproof action camera with 5.3K60 Ultra HD video, 27MP photos, HyperSmooth 5.0 stabilization, and dual LCD screens.",
        price = 499.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&h=400&fit=crop"
        ),
        sellerId = "seller9",
        sellerName = "Gadget Hub",
        category = "Electronics",
        stock = 50,
        salesCount = 700,
        shippingFrom = "Shenzhen, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express", "DHL"),
        rating = 4.8f,
        reviewCount = 600
    ),
    Product(
        id = "27",
        name = "YETI Tundra 45 Cooler",
        description = "Indestructible cooler with extra-thick walls and PermaFrost™ Insulation for unmatched ice retention. Bear-resistant design.",
        price = 299.99,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=400&h=400&fit=crop"
        ),
        sellerId = "seller11",
        sellerName = "Outdoor Gear Supply",
        category = "Outdoor",
        stock = 60,
        salesCount = 400,
        shippingFrom = "Guangzhou, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.9f,
        reviewCount = 350
    ),
    Product(
        id = "28",
        name = "Apple Watch Series 8",
        description = "Advanced health sensors and apps, so you can take an ECG, measure heart rate and blood oxygen, and track temperature changes.",
        price = 399.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&h=400&fit=crop"
        ),
        sellerId = "seller1",
        sellerName = "TechStore Official",
        category = "Electronics",
        stock = 100,
        salesCount = 1100,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.8f,
        reviewCount = 900
    ),
    Product(
        id = "29",
        name = "Bose QuietComfort 45 Headphones",
        description = "Wireless, Bluetooth, noise-cancelling headphones with high-fidelity audio. Aware Mode for transparency. Up to 24 hours of battery life.",
        price = 329.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1606400082777-ef05f3c5cde2?w=400&h=400&fit=crop"
        ),
        sellerId = "seller1",
        sellerName = "TechStore Official",
        category = "Electronics",
        stock = 70,
        salesCount = 850,
        shippingFrom = "Shanghai, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express", "JD Logistics"),
        rating = 4.8f,
        reviewCount = 750
    ),
    Product(
        id = "30",
        name = "Osprey Atmos AG 65 Men's Backpacking Backpack",
        description = "Anti-Gravity suspension system for outstanding fit and ventilation. Perfect for weekend or week-long trips. Integrated raincover.",
        price = 270.00,
        currency = "USDC",
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400&h=400&fit=crop"
        ),
        sellerId = "seller11",
        sellerName = "Outdoor Gear Supply",
        category = "Outdoor",
        stock = 50,
        salesCount = 300,
        shippingFrom = "Guangzhou, China",
        shippingTo = listOf("Worldwide"),
        shippingMethods = listOf("SF Express"),
        rating = 4.9f,
        reviewCount = 250
    )
)
