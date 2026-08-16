-- 初始化秒杀商品（INSERT IGNORE 保证幂等，重复执行不会报错）
INSERT IGNORE INTO `seckill_goods` (`id`, `name`, `description`, `price`, `stock`, `start_time`, `end_time`) VALUES
(1, 'iPhone 15 Pro',   'Apple iPhone 15 Pro 256G',      8999.00, 100, '2025-01-01 00:00:00', '2030-01-01 00:00:00'),
(2, '小米14',           '小米14 12G+256G 黑色',          3999.00, 50,  '2025-01-01 00:00:00', '2030-01-01 00:00:00'),
(3, 'AirPods Pro 2',    'Apple AirPods Pro 第二代',       1899.00, 30,  '2025-01-01 00:00:00', '2030-01-01 00:00:00');
